package com.navisow.docusign.domain.envelope;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.navisow.docusign.domain.document.DocumentRecord;
import com.navisow.docusign.domain.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "envelope_record")
public class EnvelopeRecord {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentRecord document;

    private String docuSignEnvelopeId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EnvelopeStatus status;

    private String subject;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected EnvelopeRecord() {}

    public EnvelopeRecord(AppUser user, DocumentRecord document, String subject) {
        this.user = user;
        this.document = document;
        this.subject = subject;
        this.status = EnvelopeStatus.DRAFT;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public DocumentRecord getDocument() { return document; }
    public String getDocuSignEnvelopeId() { return docuSignEnvelopeId; }
    public void setDocuSignEnvelopeId(String v) { this.docuSignEnvelopeId = v; }
    public EnvelopeStatus getStatus() { return status; }
    public void setStatus(EnvelopeStatus status) { this.status = status; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum EnvelopeStatus {
        DRAFT, SENT, DELIVERED, COMPLETED, DECLINED, VOIDED
    }
}
