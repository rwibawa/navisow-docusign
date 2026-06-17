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
@Table(name = "template_version")
public class TemplateVersion {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private DocuSignTemplate template;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(columnDefinition = "jsonb")
    private String definition;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TemplateVersion() {
    }

    public TemplateVersion(DocuSignTemplate template, Integer versionNumber, String definition, boolean isActive) {
        this.template = template;
        this.versionNumber = versionNumber;
        this.definition = definition;
        this.isActive = isActive;
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

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public String getDefinition() {
        return definition;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
