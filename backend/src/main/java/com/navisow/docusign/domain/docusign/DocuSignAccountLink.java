package com.navisow.docusign.domain.docusign;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.navisow.docusign.domain.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "docusign_account_link",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "account_id"}))
public class DocuSignAccountLink {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String baseUri;

    /** Raw access token — encrypt-at-rest before deploying to production. */
    @Column(columnDefinition = "TEXT")
    private String accessTokenCipher;

    /** Raw refresh token — encrypt-at-rest before deploying to production. */
    @Column(columnDefinition = "TEXT")
    private String refreshTokenCipher;

    private Instant tokenExpiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected DocuSignAccountLink() {}

    public DocuSignAccountLink(AppUser user, String accountId, String baseUri) {
        this.user = user;
        this.accountId = accountId;
        this.baseUri = baseUri;
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
    public String getAccountId() { return accountId; }
    public String getBaseUri() { return baseUri; }
    public void setBaseUri(String baseUri) { this.baseUri = baseUri; }
    public String getAccessTokenCipher() { return accessTokenCipher; }
    public void setAccessTokenCipher(String v) { this.accessTokenCipher = v; }
    public String getRefreshTokenCipher() { return refreshTokenCipher; }
    public void setRefreshTokenCipher(String v) { this.refreshTokenCipher = v; }
    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(Instant v) { this.tokenExpiresAt = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
