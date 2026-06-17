package com.navisow.docusign.domain.reporting;

import com.navisow.docusign.domain.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "analytics_snapshot")
public class AnalyticsSnapshot {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private String metricType;

    @Column(nullable = false)
    private int envelopeCount;

    @Column(nullable = false)
    private int signedCount;

    @Column(nullable = false)
    private int pendingCount;

    private Long averageTimeToSignSeconds;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AnalyticsSnapshot() {
    }

    public AnalyticsSnapshot(
        AppUser user,
        LocalDate snapshotDate,
        String metricType,
        int envelopeCount,
        int signedCount,
        int pendingCount,
        Long averageTimeToSignSeconds) {
        this.user = user;
        this.snapshotDate = snapshotDate;
        this.metricType = metricType;
        this.envelopeCount = envelopeCount;
        this.signedCount = signedCount;
        this.pendingCount = pendingCount;
        this.averageTimeToSignSeconds = averageTimeToSignSeconds;
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

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public String getMetricType() {
        return metricType;
    }

    public int getEnvelopeCount() {
        return envelopeCount;
    }

    public int getSignedCount() {
        return signedCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public Long getAverageTimeToSignSeconds() {
        return averageTimeToSignSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
