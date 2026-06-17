package com.navisow.docusign.domain.admin;

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
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "user_admin_state")
public class UserAdminState {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false)
    private boolean suspended;

    private String suspensionReason;

    private Instant suspendedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserAdminState() {
    }

    public UserAdminState(AppUser user) {
        this.user = user;
        this.suspended = false;
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

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void suspend(String reason) {
        this.suspended = true;
        this.suspensionReason = reason;
        this.suspendedAt = Instant.now();
    }

    public void unsuspend() {
        this.suspended = false;
        this.suspensionReason = null;
        this.suspendedAt = null;
    }
}
