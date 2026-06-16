package com.navisow.docusign.domain.envelope;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "envelope_event")
public class EnvelopeEvent {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "envelope_id", nullable = false)
    private EnvelopeRecord envelope;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected EnvelopeEvent() {}

    public EnvelopeEvent(EnvelopeRecord envelope, String eventType,
                         String rawPayload, Instant occurredAt) {
        this.envelope = envelope;
        this.eventType = eventType;
        this.rawPayload = rawPayload;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public EnvelopeRecord getEnvelope() { return envelope; }
    public String getEventType() { return eventType; }
    public String getRawPayload() { return rawPayload; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
