package com.navisow.docusign.domain.template;

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
@Table(name = "template_recipient")
public class TemplateRecipient {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private DocuSignTemplate template;

    @Column(nullable = false)
    private String roleId;

    @Column(nullable = false)
    private String recipientName;

    private String recipientEmail;

    @Column(nullable = false)
    private String recipientType;

    @Column(nullable = false)
    private int sequenceOrder = 1;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TemplateRecipient() {
    }

    public TemplateRecipient(
        DocuSignTemplate template,
        String roleId,
        String recipientName,
        String recipientEmail,
        String recipientType,
        int sequenceOrder) {
        this.template = template;
        this.roleId = roleId;
        this.recipientName = recipientName;
        this.recipientEmail = recipientEmail;
        this.recipientType = recipientType;
        this.sequenceOrder = sequenceOrder;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public DocuSignTemplate getTemplate() {
        return template;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
