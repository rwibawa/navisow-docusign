package com.navisow.docusign.api.reporting;

import com.navisow.docusign.domain.reporting.AuditLog;
import com.navisow.docusign.domain.reporting.ReportingService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportingService.getDashboard(startDate, endDate));
    }

    @GetMapping("/user-stats")
    public ResponseEntity<Map<String, Object>> userStats(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(reportingService.getUserStats(jwt));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Map<String, Object>> auditLog(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> result = reportingService.getAuditLog(PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of(
            "content", result.getContent().stream().map(this::toMap).toList(),
            "page", result.getNumber(),
            "size", result.getSize(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages()));
    }

    private Map<String, Object> toMap(AuditLog log) {
        return Map.of(
            "id", log.getId(),
            "userId", log.getUser() == null ? "" : log.getUser().getId().toString(),
            "action", log.getAction(),
            "resourceType", log.getResourceType(),
            "resourceId", log.getResourceId() == null ? "" : log.getResourceId(),
            "oldValue", log.getOldValue() == null ? "" : log.getOldValue(),
            "newValue", log.getNewValue() == null ? "" : log.getNewValue(),
            "createdAt", log.getCreatedAt());
    }
}
