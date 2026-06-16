package com.navisow.docusign.integration.docusign;

import java.io.InputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.navisow.docusign.domain.document.DocumentRecord;
import com.navisow.docusign.domain.document.DocumentService;
import com.navisow.docusign.domain.docusign.DocuSignAccountLink;
import com.navisow.docusign.domain.docusign.DocuSignAccountLinkRepository;
import com.navisow.docusign.domain.envelope.EnvelopeRecord;
import com.navisow.docusign.domain.envelope.EnvelopeRecord.EnvelopeStatus;
import com.navisow.docusign.domain.envelope.EnvelopeRecordRepository;
import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.storage.StorageService;

/**
 * Integrates with DocuSign eSignature REST API to create and manage envelopes.
 * All calls are made on behalf of the app user's connected DocuSign account.
 */
@Service
public class DocuSignEnvelopeService {

    private final DocuSignAccountLinkRepository linkRepository;
    private final EnvelopeRecordRepository envelopeRepository;
    private final DocumentService documentService;
    private final StorageService storageService;
    private final DocuSignTokenService tokenService;
    private final RestClient restClient;

    public DocuSignEnvelopeService(DocuSignAccountLinkRepository linkRepository,
                                   EnvelopeRecordRepository envelopeRepository,
                                   DocumentService documentService,
                                   StorageService storageService,
                                   DocuSignTokenService tokenService,
                                   RestClient.Builder builder) {
        this.linkRepository = linkRepository;
        this.envelopeRepository = envelopeRepository;
        this.documentService = documentService;
        this.storageService = storageService;
        this.tokenService = tokenService;
        this.restClient = builder.build();
    }

    // -------------------------------------------------------------------------
    // Send envelope
    // -------------------------------------------------------------------------

    @Transactional
    public EnvelopeRecord sendEnvelope(AppUser user, SendEnvelopeRequest request) {
        DocuSignAccountLink link = requireLink(user);

        // Build DocuSign envelope definition
        Map<String, Object> envelopeDef = buildEnvelopeDef(user, request);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
            .uri(link.getBaseUri() + "/v2.1/accounts/{accountId}/envelopes",
                link.getAccountId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + link.getAccessTokenCipher())
            .contentType(MediaType.APPLICATION_JSON)
            .body(envelopeDef)
            .retrieve()
            .body(Map.class);

        if (response == null) throw new IllegalStateException("Empty response from DocuSign");

        String dsEnvelopeId = (String) response.get("envelopeId");

        // Persist envelope record
        DocumentRecord doc = request.documentId() != null
            ? documentService.getForUser(request.documentId(), user.getId())
            : null;

        EnvelopeRecord envelope = new EnvelopeRecord(user, doc, request.subject());
        envelope.setDocuSignEnvelopeId(dsEnvelopeId);
        envelope.setStatus(EnvelopeStatus.SENT);

        return envelopeRepository.save(envelope);
    }

    // -------------------------------------------------------------------------
    // Embedded signing
    // -------------------------------------------------------------------------

    public String createEmbeddedSigningUrl(AppUser user, String envelopeId,
                                           String recipientEmail, String recipientName,
                                           String clientUserId, String returnUrl) {
        DocuSignAccountLink link = requireLink(user);

        Map<String, Object> body = Map.of(
            "returnUrl", returnUrl,
            "authenticationMethod", "none",
            "email", recipientEmail,
            "userName", recipientName,
            "clientUserId", clientUserId
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
            .uri(link.getBaseUri() + "/v2.1/accounts/{accountId}/envelopes/{envelopeId}/views/recipient",
                link.getAccountId(), envelopeId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + link.getAccessTokenCipher())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map.class);

        if (response == null) throw new IllegalStateException("Empty response from DocuSign");
        return (String) response.get("url");
    }

    // -------------------------------------------------------------------------
    // Certificate of completion
    // -------------------------------------------------------------------------

    public InputStream downloadCertificate(AppUser user, String envelopeId) {
        DocuSignAccountLink link = requireLink(user);
        return restClient.get()
            .uri(link.getBaseUri()
                + "/v2.1/accounts/{accountId}/envelopes/{envelopeId}/documents/certificate",
                link.getAccountId(), envelopeId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + link.getAccessTokenCipher())
            .retrieve()
            .body(InputStream.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DocuSignAccountLink requireLink(AppUser user) {
        DocuSignAccountLink link = linkRepository.findFirstByUser_Id(user.getId())
            .orElseThrow(() -> new IllegalStateException(
                "No DocuSign account connected for user " + user.getId()));
        return tokenService.refreshIfExpired(link);
    }

    private Map<String, Object> buildEnvelopeDef(AppUser user, SendEnvelopeRequest request) {
        List<Map<String, Object>> recipients = request.recipients().stream()
            .map(r -> {
                Map<String, Object> signer = new LinkedHashMap<>();
                signer.put("email", r.email());
                signer.put("name", r.name());
                signer.put("recipientId", String.valueOf(r.recipientId()));
                signer.put("routingOrder", String.valueOf(r.routingOrder()));
                if (r.clientUserId() != null) {
                    signer.put("clientUserId", r.clientUserId());
                }
                return signer;
            })
            .toList();

        Map<String, Object> def = new LinkedHashMap<>();
        def.put("emailSubject", request.subject());
        def.put("status", "sent");

        // Attach uploaded document if provided
        if (request.documentId() != null) {
            DocumentRecord doc = documentService.getForUser(request.documentId(), user.getId());
            try (InputStream content = storageService.retrieve(doc.getStorageKey())) {
                String base64 = Base64.getEncoder().encodeToString(content.readAllBytes());
                def.put("documents", List.of(Map.of(
                    "documentId", "1",
                    "name", doc.getFileName(),
                    "documentBase64", base64
                )));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read document content", e);
            }
        }

        def.put("recipients", Map.of("signers", recipients));

        // Optional: reminders / expiration
        if (request.reminderDays() != null || request.expirationDays() != null) {
            Map<String, Object> notification = new LinkedHashMap<>();
            if (request.reminderDays() != null) {
                notification.put("reminders", Map.of(
                    "reminderEnabled", "true",
                    "reminderDelay", String.valueOf(request.reminderDays()),
                    "reminderFrequency", "2"
                ));
            }
            if (request.expirationDays() != null) {
                notification.put("expirations", Map.of(
                    "expireEnabled", "true",
                    "expireAfter", String.valueOf(request.expirationDays()),
                    "expireWarn", "2"
                ));
            }
            def.put("notification", notification);
        }

        return def;
    }

    // -------------------------------------------------------------------------
    // Request/response types
    // -------------------------------------------------------------------------

    public record RecipientRequest(
        int recipientId,
        int routingOrder,
        String name,
        String email,
        String clientUserId
    ) {}

    public record SendEnvelopeRequest(
        UUID documentId,
        String subject,
        List<RecipientRequest> recipients,
        Integer reminderDays,
        Integer expirationDays
    ) {}
}
