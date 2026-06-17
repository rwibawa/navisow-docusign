package com.navisow.docusign.api.admin;

import com.navisow.docusign.domain.admin.AdminService;
import com.navisow.docusign.domain.admin.AdminUserNotFoundException;
import com.navisow.docusign.domain.admin.AdminService.AdminUserDetails;
import com.navisow.docusign.domain.admin.UserAdminState;
import com.navisow.docusign.domain.user.AppUser;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        Page<AppUser> users = adminService.listUsers(PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of(
            "content", users.getContent().stream().map(this::toUserMap).toList(),
            "page", users.getNumber(),
            "size", users.getSize(),
            "totalElements", users.getTotalElements(),
            "totalPages", users.getTotalPages()));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> userDetails(@PathVariable UUID id) {
        AdminUserDetails details = adminService.getUserDetails(id);
        return ResponseEntity.ok(Map.of(
            "user", toUserMap(details.user()),
            "adminState", toAdminStateMap(details.state()),
            "templateCount", details.templateCount(),
            "operationCount", details.operationCount(),
            "completedOperationCount", details.completedOperationCount()));
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<Map<String, Object>> suspendUser(
        @PathVariable UUID id,
        @RequestBody SuspendRequest request) {
        UserAdminState state = adminService.suspendUser(id, request.reason());
        return ResponseEntity.ok(toAdminStateMap(state));
    }

    @PutMapping("/users/{id}/unsuspend")
    public ResponseEntity<Map<String, Object>> unsuspendUser(@PathVariable UUID id) {
        UserAdminState state = adminService.unsuspendUser(id);
        return ResponseEntity.ok(toAdminStateMap(state));
    }

    @PostMapping("/users/{id}/reset-tokens")
    public ResponseEntity<Map<String, Object>> resetTokens(@PathVariable UUID id) {
        adminService.resetUserTokens(id);
        return ResponseEntity.ok(Map.of("userId", id, "reset", true));
    }

    @GetMapping("/users/{id}/data-export")
    public ResponseEntity<Map<String, Object>> exportUserData(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.exportUserData(id));
    }

    @ExceptionHandler(AdminUserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(AdminUserNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    private Map<String, Object> toUserMap(AppUser user) {
        return Map.of(
            "id", user.getId(),
            "subject", user.getSubject(),
            "email", user.getEmail() == null ? "" : user.getEmail(),
            "displayName", user.getDisplayName() == null ? "" : user.getDisplayName(),
            "createdAt", user.getCreatedAt(),
            "updatedAt", user.getUpdatedAt());
    }

    private Map<String, Object> toAdminStateMap(UserAdminState state) {
        return Map.of(
            "suspended", state.isSuspended(),
            "reason", state.getSuspensionReason() == null ? "" : state.getSuspensionReason(),
            "suspendedAt", state.getSuspendedAt() == null ? "" : state.getSuspendedAt().toString(),
            "updatedAt", state.getUpdatedAt());
    }

    public record SuspendRequest(@NotBlank String reason) {
    }
}
