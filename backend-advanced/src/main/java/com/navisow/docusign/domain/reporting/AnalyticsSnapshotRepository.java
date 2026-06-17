package com.navisow.docusign.domain.reporting;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {

    List<AnalyticsSnapshot> findBySnapshotDateBetweenOrderBySnapshotDateAsc(LocalDate startDate, LocalDate endDate);

    List<AnalyticsSnapshot> findByUserIdOrderBySnapshotDateDesc(UUID userId);

    boolean existsByUserIdAndSnapshotDateAndMetricType(UUID userId, LocalDate snapshotDate, String metricType);
}
