package com.navisow.docusign.api.bulk;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.navisow.docusign.domain.bulk.BulkEnvelopeService;
import com.navisow.docusign.domain.bulk.BulkEnvelopeService.BulkOperationDetail;
import com.navisow.docusign.domain.bulk.BulkEnvelopeService.CreateBulkItemCommand;
import com.navisow.docusign.domain.bulk.BulkEnvelopeService.CreateBulkOperationCommand;
import com.navisow.docusign.domain.bulk.BulkOperation;
import com.navisow.docusign.domain.bulk.BulkOperationItem;
import com.navisow.docusign.domain.bulk.BulkOperationNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/bulk-operations")
@Validated
public class BulkOperationController {

    private final BulkEnvelopeService bulkEnvelopeService;

    public BulkOperationController(BulkEnvelopeService bulkEnvelopeService) {
        this.bulkEnvelopeService = bulkEnvelopeService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid CreateBulkOperationRequest request) {
        BulkOperation operation = bulkEnvelopeService.createBulkOperation(
            jwt,
            new CreateBulkOperationCommand(
                request.name(),
                request.items().stream()
                    .map(item -> new CreateBulkItemCommand(item.documentId(), item.recipientListJson()))
                    .toList()));
        return ResponseEntity.ok(toMap(operation));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        Page<BulkOperation> operations = bulkEnvelopeService.listForUser(jwt, PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of(
            "content", operations.getContent().stream().map(this::toMap).toList(),
            "page", operations.getNumber(),
            "size", operations.getSize(),
            "totalElements", operations.getTotalElements(),
            "totalPages", operations.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id) {
        BulkOperationDetail detail = bulkEnvelopeService.getForUser(jwt, id);
        return ResponseEntity.ok(Map.of(
            "operation", toMap(detail.operation()),
            "items", detail.items().stream().map(this::toItemMap).toList()));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Map<String, Object>> process(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id) {
        BulkOperation operation = bulkEnvelopeService.processBulkOperation(jwt, id);
        return ResponseEntity.ok(toMap(operation));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id) {
        BulkOperation operation = bulkEnvelopeService.cancelBulkOperation(jwt, id);
        return ResponseEntity.ok(toMap(operation));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, Object>> retry(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id) {
        BulkOperation operation = bulkEnvelopeService.retryFailedItems(jwt, id);
        return ResponseEntity.ok(toMap(operation));
    }

    @ExceptionHandler(BulkOperationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(BulkOperationNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    private Map<String, Object> toMap(BulkOperation operation) {
        return Map.of(
            "id", operation.getId(),
            "name", operation.getName(),
            "status", operation.getStatus().name(),
            "totalCount", operation.getTotalCount(),
            "processedCount", operation.getProcessedCount(),
            "successCount", operation.getSuccessCount(),
            "failureCount", operation.getFailureCount(),
            "createdAt", operation.getCreatedAt(),
            "startedAt", operation.getStartedAt() == null ? "" : operation.getStartedAt().toString(),
            "completedAt", operation.getCompletedAt() == null ? "" : operation.getCompletedAt().toString());
    }

    private Map<String, Object> toItemMap(BulkOperationItem item) {
        return Map.of(
            "id", item.getId(),
            "documentId", item.getDocumentId() == null ? "" : item.getDocumentId().toString(),
            "recipientList", item.getRecipientList() == null ? "" : item.getRecipientList(),
            "status", item.getStatus().name(),
            "envelopeId", item.getEnvelopeId() == null ? "" : item.getEnvelopeId(),
            "errorMessage", item.getErrorMessage() == null ? "" : item.getErrorMessage(),
            "createdAt", item.getCreatedAt(),
            "completedAt", item.getCompletedAt() == null ? "" : item.getCompletedAt().toString());
    }

    public record CreateBulkOperationRequest(
        @NotBlank String name,
        @NotNull List<@Valid CreateBulkOperationItemRequest> items) {
    }

    public record CreateBulkOperationItemRequest(
        UUID documentId,
        String recipientListJson) {
    }
}
