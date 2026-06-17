package com.navisow.docusign.integration.bulk;

import com.navisow.docusign.domain.bulk.BulkOperation;
import com.navisow.docusign.domain.bulk.BulkOperationItem;
import com.navisow.docusign.domain.bulk.BulkOperationItemRepository;
import com.navisow.docusign.domain.bulk.BulkOperationRepository;
import com.navisow.docusign.domain.docusign.DocuSignAccountLink;
import com.navisow.docusign.domain.docusign.DocuSignAccountLinkRepository;
import com.navisow.docusign.integration.docusign.DocuSignTokenService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class BulkEnvelopeProcessor {

    private final BulkOperationRepository bulkOperationRepository;
    private final BulkOperationItemRepository bulkOperationItemRepository;
    private final DocuSignAccountLinkRepository accountLinkRepository;
    private final DocuSignTokenService tokenService;
    private final RestClient restClient;

    public BulkEnvelopeProcessor(
        BulkOperationRepository bulkOperationRepository,
        BulkOperationItemRepository bulkOperationItemRepository,
        DocuSignAccountLinkRepository accountLinkRepository,
        DocuSignTokenService tokenService,
        RestClient.Builder builder) {
        this.bulkOperationRepository = bulkOperationRepository;
        this.bulkOperationItemRepository = bulkOperationItemRepository;
        this.accountLinkRepository = accountLinkRepository;
        this.tokenService = tokenService;
        this.restClient = builder.build();
    }

    @Async
    @Transactional
    public void processBulkOperationAsync(BulkOperation operation) {
        List<BulkOperationItem> items = bulkOperationItemRepository.findByBulkOperationId(
            operation.getId());
        items = items.stream()
            .filter(item -> item.getStatus() == BulkOperationItem.BulkItemStatus.PENDING)
            .toList();

        DocuSignAccountLink link = accountLinkRepository.findFirstByUser_Id(operation.getUser().getId())
            .orElse(null);
        if (link == null) {
            markOperationFailed(operation, "No DocuSign account connected");
            return;
        }

        link = tokenService.refreshIfExpired(link);
        operation.markInProgress();
        bulkOperationRepository.save(operation);

        int successCount = 0;
        int failureCount = 0;
        for (BulkOperationItem item : items) {
            try {
                processItemAgainstDocuSign(link, item);
                item.markCompleted();
                successCount++;
            } catch (RestClientException | IllegalStateException ex) {
                item.markFailed(ex.getMessage());
                failureCount++;
            }
            bulkOperationItemRepository.save(item);
        }

        operation.setProcessedCount(successCount + failureCount);
        operation.setSuccessCount(successCount);
        operation.setFailureCount(failureCount);

        if (failureCount == 0) {
            operation.markCompleted();
        } else {
            operation.setStatus(BulkOperation.BulkOperationStatus.FAILED);
            operation.setCompletedAt(Instant.now());
        }

        bulkOperationRepository.save(operation);
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void processScheduledBulkOperations() {
        List<BulkOperation> inProgressOperations = bulkOperationRepository
            .findByStatus(BulkOperation.BulkOperationStatus.IN_PROGRESS);
        for (BulkOperation operation : inProgressOperations) {
            processBulkOperationAsync(operation);
        }
    }

    private void processItemAgainstDocuSign(DocuSignAccountLink link, BulkOperationItem item) {
        Map<String, Object> envelopeDefinition = buildEnvelopeDefinition(item);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
            .uri(link.getBaseUri() + "/v2.1/accounts/{accountId}/envelopes", link.getAccountId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + link.getAccessTokenCipher())
            .body(envelopeDefinition)
            .retrieve()
            .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Empty response from DocuSign envelope creation");
        }

        String envelopeId = (String) response.get("envelopeId");
        if (envelopeId == null || envelopeId.isBlank()) {
            throw new IllegalStateException("No envelopeId in DocuSign response");
        }

        item.setEnvelopeId(envelopeId);
    }

    private Map<String, Object> buildEnvelopeDefinition(BulkOperationItem item) {
        return Map.of(
            "emailSubject", "Please sign this document",
            "status", "sent",
            "recipients", Map.of(),
            "documents", List.of(
                Map.of(
                    "documentId", item.getDocumentId() == null ? "1" : item.getDocumentId().toString(),
                    "name", "Document",
                    "fileExtension", "pdf",
                    "documentBase64", "")));
    }

    private void markOperationFailed(BulkOperation operation, @SuppressWarnings("unused") String reason) {
        operation.setStatus(BulkOperation.BulkOperationStatus.FAILED);
        operation.setCompletedAt(Instant.now());
        bulkOperationRepository.save(operation);
    }
}
