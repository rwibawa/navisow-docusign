package com.navisow.docusign.domain.reporting;

import com.navisow.docusign.domain.bulk.BulkOperation;
import com.navisow.docusign.domain.bulk.BulkOperationRepository;
import com.navisow.docusign.domain.template.DocuSignTemplateRepository;
import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.domain.user.AppUserRepository;
import com.navisow.docusign.domain.user.AppUserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {

    private static final String METRIC_TYPE_DAILY = "DAILY";

    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final DocuSignTemplateRepository templateRepository;
    private final BulkOperationRepository bulkOperationRepository;

    public ReportingService(
        AnalyticsSnapshotRepository analyticsSnapshotRepository,
        AuditLogRepository auditLogRepository,
        AppUserRepository appUserRepository,
        AppUserService appUserService,
        DocuSignTemplateRepository templateRepository,
        BulkOperationRepository bulkOperationRepository) {
        this.analyticsSnapshotRepository = analyticsSnapshotRepository;
        this.auditLogRepository = auditLogRepository;
        this.appUserRepository = appUserRepository;
        this.appUserService = appUserService;
        this.templateRepository = templateRepository;
        this.bulkOperationRepository = bulkOperationRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(LocalDate startDate, LocalDate endDate) {
        return generateDailyReport(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateDailyReport(LocalDate startDate, LocalDate endDate) {
        List<AnalyticsSnapshot> snapshots = analyticsSnapshotRepository
            .findBySnapshotDateBetweenOrderBySnapshotDateAsc(startDate, endDate);

        int envelopeCount = snapshots.stream().mapToInt(AnalyticsSnapshot::getEnvelopeCount).sum();
        int signedCount = snapshots.stream().mapToInt(AnalyticsSnapshot::getSignedCount).sum();
        int pendingCount = snapshots.stream().mapToInt(AnalyticsSnapshot::getPendingCount).sum();
        long avgTimeToSignSeconds = averageTimeToSignSeconds(snapshots);
        double completionRate = envelopeCount == 0 ? 0.0d : (double) signedCount / envelopeCount;

        return Map.of(
            "startDate", startDate,
            "endDate", endDate,
            "snapshotCount", snapshots.size(),
            "envelopeCount", envelopeCount,
            "signedCount", signedCount,
            "pendingCount", pendingCount,
            "averageTimeToSignSeconds", avgTimeToSignSeconds,
            "completionRate", completionRate,
            "content", snapshots.stream().map(this::toSnapshotMap).toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(Jwt jwt) {
        return generateUserReport(appUserService.getOrCreateUser(jwt));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateUserReport(AppUser user) {
        long templateCount = templateRepository.countByUserId(user.getId());
        long operationCount = bulkOperationRepository.countByUserId(user.getId());
        long completedCount = bulkOperationRepository.countByUserIdAndStatus(user.getId(), BulkOperation.BulkOperationStatus.COMPLETED);
        long pendingCount = bulkOperationRepository.countByUserIdAndStatus(user.getId(), BulkOperation.BulkOperationStatus.IN_PROGRESS)
            + bulkOperationRepository.countByUserIdAndStatus(user.getId(), BulkOperation.BulkOperationStatus.CREATED);
        List<AnalyticsSnapshot> recentSnapshots = analyticsSnapshotRepository.findByUserIdOrderBySnapshotDateDesc(user.getId());
        long avgTimeToSignSeconds = averageTimeToSignSeconds(recentSnapshots);
        double completionRate = operationCount == 0 ? 0.0d : (double) completedCount / operationCount;

        return Map.of(
            "userId", user.getId(),
            "templateCount", templateCount,
            "operationCount", operationCount,
            "completedOperationCount", completedCount,
            "pendingOperationCount", pendingCount,
            "averageTimeToSignSeconds", avgTimeToSignSeconds,
            "completionRate", completionRate,
            "recentSnapshots", recentSnapshots.stream().limit(7).map(this::toSnapshotMap).toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLog(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public void logAudit(AppUser user, String action, String resourceType, String resourceId, String oldValue, String newValue) {
        auditLogRepository.save(new AuditLog(user, action, resourceType, resourceId, oldValue, newValue));
    }

    @Scheduled(cron = "0 15 0 * * *")
    @Transactional
    public void generateDailySnapshots() {
        LocalDate today = LocalDate.now();
        List<AppUser> users = appUserRepository.findAll();
        for (AppUser user : users) {
            boolean exists = analyticsSnapshotRepository.existsByUserIdAndSnapshotDateAndMetricType(
                user.getId(),
                today,
                METRIC_TYPE_DAILY);
            if (exists) {
                continue;
            }

            int envelopes = (int) bulkOperationRepository.countByUserId(user.getId());
            int signed = (int) bulkOperationRepository.countByUserIdAndStatus(user.getId(), BulkOperation.BulkOperationStatus.COMPLETED);
            int pending = (int) bulkOperationRepository.countByUserIdAndStatus(user.getId(), BulkOperation.BulkOperationStatus.IN_PROGRESS)
                + (int) bulkOperationRepository.countByUserIdAndStatus(user.getId(), BulkOperation.BulkOperationStatus.CREATED);

            analyticsSnapshotRepository.save(buildSnapshotForUser(user, today, envelopes, signed, pending));
        }
    }

    private AnalyticsSnapshot buildSnapshotForUser(
        AppUser user,
        LocalDate snapshotDate,
        int envelopes,
        int signed,
        int pending) {
        return new AnalyticsSnapshot(
            user,
            snapshotDate,
            METRIC_TYPE_DAILY,
            envelopes,
            signed,
            pending,
            null);
    }

    private long averageTimeToSignSeconds(List<AnalyticsSnapshot> snapshots) {
        return (long) snapshots.stream()
            .map(AnalyticsSnapshot::getAverageTimeToSignSeconds)
            .filter(value -> value != null && value > 0)
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
    }

    private Map<String, Object> toSnapshotMap(AnalyticsSnapshot snapshot) {
        Long avgTimeToSign = snapshot.getAverageTimeToSignSeconds();
        return Map.of(
            "id", snapshot.getId(),
            "userId", snapshot.getUser().getId(),
            "snapshotDate", snapshot.getSnapshotDate(),
            "metricType", snapshot.getMetricType(),
            "envelopeCount", snapshot.getEnvelopeCount(),
            "signedCount", snapshot.getSignedCount(),
            "pendingCount", snapshot.getPendingCount(),
            "averageTimeToSignSeconds", avgTimeToSign == null ? 0L : avgTimeToSign,
            "createdAt", snapshot.getCreatedAt());
    }
}
