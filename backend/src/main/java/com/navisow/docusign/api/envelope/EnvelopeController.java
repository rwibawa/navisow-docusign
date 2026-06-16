package com.navisow.docusign.api.envelope;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.navisow.docusign.domain.envelope.EnvelopeEvent;
import com.navisow.docusign.domain.envelope.EnvelopeEventRepository;
import com.navisow.docusign.domain.envelope.EnvelopeRecord;
import com.navisow.docusign.domain.envelope.EnvelopeRecordRepository;
import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.domain.user.AppUserService;
import com.navisow.docusign.integration.docusign.DocuSignEnvelopeService;
import com.navisow.docusign.integration.docusign.DocuSignEnvelopeService.RecipientRequest;
import com.navisow.docusign.integration.docusign.DocuSignEnvelopeService.SendEnvelopeRequest;

@RestController
@RequestMapping("/api/envelopes")
public class EnvelopeController {

    private final DocuSignEnvelopeService envelopeService;
    private final EnvelopeRecordRepository envelopeRepository;
    private final EnvelopeEventRepository eventRepository;
    private final AppUserService userService;

    public EnvelopeController(DocuSignEnvelopeService envelopeService,
                               EnvelopeRecordRepository envelopeRepository,
                               EnvelopeEventRepository eventRepository,
                               AppUserService userService) {
        this.envelopeService = envelopeService;
        this.envelopeRepository = envelopeRepository;
        this.eventRepository = eventRepository;
        this.userService = userService;
    }

    /** Send a new envelope. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> send(
            @RequestBody SendRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        AppUser user = userService.getOrCreateUser(jwt);
        EnvelopeRecord envelope = envelopeService.sendEnvelope(user, new SendEnvelopeRequest(
            req.documentId(),
            req.subject(),
            req.recipients().stream()
                .map(r -> new RecipientRequest(r.recipientId(), r.routingOrder(),
                    r.name(), r.email(), r.clientUserId()))
                .toList(),
            req.reminderDays(),
            req.expirationDays()
        ));

        return ResponseEntity.ok(envelopeToMap(envelope));
    }

    /** List envelopes for the authenticated user. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        AppUser user = userService.getOrCreateUser(jwt);
        Page<EnvelopeRecord> results = envelopeRepository.findByUser_Id(
            user.getId(),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(Map.of(
            "content", results.getContent().stream().map(this::envelopeToMap).toList(),
            "totalElements", results.getTotalElements(),
            "totalPages", results.getTotalPages(),
            "page", page
        ));
    }

    /** Get single envelope with event timeline. */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        AppUser user = userService.getOrCreateUser(jwt);
        EnvelopeRecord envelope = envelopeRepository.findById(id)
            .filter(e -> e.getUser().getId().equals(user.getId()))
            .orElse(null);

        if (envelope == null) {
            return ResponseEntity.notFound().build();
        }

        List<EnvelopeEvent> events = eventRepository
            .findByEnvelope_IdOrderByOccurredAtDesc(id);

        Map<String, Object> body = new java.util.LinkedHashMap<>(envelopeToMap(envelope));
        body.put("events", events.stream().map(ev -> Map.of(
            "id", ev.getId().toString(),
            "eventType", ev.getEventType(),
            "occurredAt", ev.getOccurredAt().toString()
        )).toList());

        return ResponseEntity.ok(body);
    }

    /** Create an embedded signing URL for a recipient. */
    @PostMapping("/{id}/signing-url")
    public ResponseEntity<Map<String, String>> signingUrl(
            @PathVariable UUID id,
            @RequestBody SigningUrlRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        AppUser user = userService.getOrCreateUser(jwt);
        EnvelopeRecord envelope = envelopeRepository.findById(id)
            .filter(e -> e.getUser().getId().equals(user.getId()))
            .orElse(null);

        if (envelope == null || envelope.getDocuSignEnvelopeId() == null) {
            return ResponseEntity.notFound().build();
        }

        String url = envelopeService.createEmbeddedSigningUrl(
            user,
            envelope.getDocuSignEnvelopeId(),
            req.recipientEmail(),
            req.recipientName(),
            req.clientUserId(),
            req.returnUrl()
        );

        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Download certificate of completion PDF. */
    @GetMapping("/{id}/certificate")
    public ResponseEntity<InputStreamResource> certificate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        AppUser user = userService.getOrCreateUser(jwt);
        EnvelopeRecord envelope = envelopeRepository.findById(id)
            .filter(e -> e.getUser().getId().equals(user.getId()))
            .orElse(null);

        if (envelope == null || envelope.getDocuSignEnvelopeId() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"certificate-" + id + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(new InputStreamResource(
                envelopeService.downloadCertificate(user, envelope.getDocuSignEnvelopeId())));
    }

    // -------------------------------------------------------------------------
    // Request / response records
    // -------------------------------------------------------------------------

    public record RecipientDto(int recipientId, int routingOrder,
                               String name, String email, String clientUserId) {}

    public record SendRequest(UUID documentId, String subject,
                              List<RecipientDto> recipients,
                              Integer reminderDays, Integer expirationDays) {}

    public record SigningUrlRequest(String recipientEmail, String recipientName,
                                   String clientUserId, String returnUrl) {}

    private Map<String, Object> envelopeToMap(EnvelopeRecord e) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", e.getId().toString());
        m.put("subject", e.getSubject());
        m.put("status", e.getStatus().name());
        m.put("docuSignEnvelopeId", e.getDocuSignEnvelopeId());
        m.put("createdAt", e.getCreatedAt().toString());
        m.put("updatedAt", e.getUpdatedAt().toString());
        return m;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleBadState(IllegalStateException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }
}
