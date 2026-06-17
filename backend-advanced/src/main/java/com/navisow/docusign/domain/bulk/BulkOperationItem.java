package com.navisow.docusign.domain.bulk;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "bulk_operation_item")
public class BulkOperationItem {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bulk_operation_id", nullable = false)
    private BulkOperation bulkOperation;

    @Column(columnDefinition = "UUID")
    private UUID documentId;

    @Column(columnDefinition = "jsonb")
    private String recipientList;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BulkItemStatus status;

    private String envelopeId;

    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    protected BulkOperationItem() {
    }

    public BulkOperationItem(BulkOperation bulkOperation, UUID documentId, String recipientList) {
        this.bulkOperation = bulkOperation;
        this.documentId = documentId;
        this.recipientList = recipientList;
        this.status = BulkItemStatus.PENDING;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getRecipientList() {
        return recipientList;
    }

    public BulkItemStatus getStatus() {
        return status;
    }

    public String getEnvelopeId() {
        return envelopeId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setEnvelopeId(String envelopeId) {
        this.envelopeId = envelopeId;
    }

    public void markCompleted() {
        this.status = BulkItemStatus.SENT;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorMsg) {
        this.status = BulkItemStatus.FAILED;
        this.errorMessage = errorMsg;
        this.completedAt = Instant.now();
    }

    public enum BulkItemStatus {
        PENDING,
        SENT,
        FAILED,
        CANCELLED
    }
}
