package com.navisow.docusign.domain.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "webhook_event")
public class WebhookEvent {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String externalEventId;

    private String envelopeId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WebhookEventStatus status;

    private String errorMessage;

    private Instant processedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected WebhookEvent() {
    }

    public WebhookEvent(String externalEventId, String envelopeId, String eventType, String payload) {
        this.externalEventId = externalEventId;
        this.envelopeId = envelopeId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = WebhookEventStatus.RECEIVED;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public String getEnvelopeId() {
        return envelopeId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public WebhookEventStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markProcessed() {
        this.status = WebhookEventStatus.PROCESSED;
        this.processedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = WebhookEventStatus.FAILED;
        this.processedAt = Instant.now();
        this.errorMessage = errorMessage;
    }

    public enum WebhookEventStatus {
        RECEIVED,
        PROCESSED,
        FAILED
    }
}
