package com.navisow.docusign.domain.bulk;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "bulk_operation")
public class BulkOperation {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BulkOperationStatus status;

    @Column(nullable = false)
    private Integer totalCount = 0;

    @Column(nullable = false)
    private Integer processedCount = 0;

    @Column(nullable = false)
    private Integer successCount = 0;

    @Column(nullable = false)
    private Integer failureCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant startedAt;

    private Instant completedAt;

    protected BulkOperation() {
    }

    public BulkOperation(AppUser user, String name, int totalCount) {
        this.user = user;
        this.name = name;
        this.totalCount = totalCount;
        this.status = BulkOperationStatus.CREATED;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public BulkOperationStatus getStatus() {
        return status;
    }

    public void setStatus(BulkOperationStatus status) {
        this.status = status;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public Integer getProcessedCount() {
        return processedCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setProcessedCount(Integer count) {
        this.processedCount = count;
    }

    public void setSuccessCount(Integer count) {
        this.successCount = count;
    }

    public void setFailureCount(Integer count) {
        this.failureCount = count;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public void markInProgress() {
        this.status = BulkOperationStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = BulkOperationStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = BulkOperationStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.processedCount = this.totalCount;
    }

    public enum BulkOperationStatus {
        CREATED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        FAILED
    }
}
