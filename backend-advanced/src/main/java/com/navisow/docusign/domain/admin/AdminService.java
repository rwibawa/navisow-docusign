package com.navisow.docusign.domain.admin;

import com.navisow.docusign.domain.bulk.BulkOperation;
import com.navisow.docusign.domain.bulk.BulkOperationRepository;
import com.navisow.docusign.domain.reporting.ReportingService;
import com.navisow.docusign.domain.template.DocuSignTemplateRepository;
import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.domain.user.AppUserRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final AppUserRepository appUserRepository;
    private final UserAdminStateRepository userAdminStateRepository;
    private final DocuSignTemplateRepository templateRepository;
    private final BulkOperationRepository bulkOperationRepository;
    private final ReportingService reportingService;

    public AdminService(
        AppUserRepository appUserRepository,
        UserAdminStateRepository userAdminStateRepository,
        DocuSignTemplateRepository templateRepository,
        BulkOperationRepository bulkOperationRepository,
        ReportingService reportingService) {
        this.appUserRepository = appUserRepository;
        this.userAdminStateRepository = userAdminStateRepository;
        this.templateRepository = templateRepository;
        this.bulkOperationRepository = bulkOperationRepository;
        this.reportingService = reportingService;
    }

    @Transactional(readOnly = true)
    public Page<AppUser> listUsers(Pageable pageable) {
        return appUserRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public AdminUserDetails getUserDetails(UUID userId) {
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new AdminUserNotFoundException(userId));
        UserAdminState state = userAdminStateRepository.findByUserId(userId).orElseGet(() -> new UserAdminState(user));

        return new AdminUserDetails(
            user,
            state,
            templateRepository.countByUserId(userId),
            bulkOperationRepository.countByUserId(userId),
            bulkOperationRepository.countByUserIdAndStatus(userId, BulkOperation.BulkOperationStatus.COMPLETED));
    }

    @Transactional
    public UserAdminState suspendUser(UUID userId, String reason) {
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new AdminUserNotFoundException(userId));
        UserAdminState state = userAdminStateRepository.findByUserId(userId)
            .orElseGet(() -> userAdminStateRepository.save(new UserAdminState(user)));

        String oldValue = "{\"suspended\":" + state.isSuspended() + "}";
        state.suspend(reason == null ? "Suspended by admin" : reason);
        UserAdminState saved = userAdminStateRepository.save(state);

        String newValue = "{\"suspended\":true,\"reason\":\"" + (reason == null ? "Suspended by admin" : reason) + "\"}";
        reportingService.logAudit(user, "USER_SUSPEND", "APP_USER", userId.toString(), oldValue, newValue);
        return saved;
    }

    @Transactional
    public UserAdminState unsuspendUser(UUID userId) {
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new AdminUserNotFoundException(userId));
        UserAdminState state = userAdminStateRepository.findByUserId(userId)
            .orElseGet(() -> userAdminStateRepository.save(new UserAdminState(user)));

        String oldValue = "{\"suspended\":" + state.isSuspended() + "}";
        state.unsuspend();
        UserAdminState saved = userAdminStateRepository.save(state);

        reportingService.logAudit(user, "USER_UNSUSPEND", "APP_USER", userId.toString(), oldValue, "{\"suspended\":false}");
        return saved;
    }

    @Transactional
    public void resetUserTokens(UUID userId) {
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new AdminUserNotFoundException(userId));
        reportingService.logAudit(user, "DOCUSIGN_TOKEN_RESET", "APP_USER", userId.toString(), null, "{\"reset\":true}");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(UUID userId) {
        AdminUserDetails details = getUserDetails(userId);
        AppUser user = details.user();
        UserAdminState state = details.state();

        return Map.of(
            "userId", user.getId(),
            "subject", user.getSubject(),
            "email", user.getEmail() == null ? "" : user.getEmail(),
            "displayName", user.getDisplayName() == null ? "" : user.getDisplayName(),
            "suspended", state.isSuspended(),
            "suspensionReason", state.getSuspensionReason() == null ? "" : state.getSuspensionReason(),
            "templateCount", details.templateCount(),
            "operationCount", details.operationCount(),
            "completedOperationCount", details.completedOperationCount());
    }

    public record AdminUserDetails(
        AppUser user,
        UserAdminState state,
        long templateCount,
        long operationCount,
        long completedOperationCount) {
    }
}
