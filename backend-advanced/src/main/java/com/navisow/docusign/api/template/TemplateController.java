package com.navisow.docusign.api.template;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.navisow.docusign.domain.template.DocuSignTemplate;
import com.navisow.docusign.domain.template.DocuSignTemplateService;
import com.navisow.docusign.domain.template.DocuSignTemplateService.CreateTemplateCommand;
import com.navisow.docusign.domain.template.DocuSignTemplateService.RecipientCommand;
import com.navisow.docusign.domain.template.DocuSignTemplateService.TemplateDetails;
import com.navisow.docusign.domain.template.DocuSignTemplateService.UpdateTemplateCommand;
import com.navisow.docusign.domain.template.TemplateNotFoundException;
import com.navisow.docusign.domain.template.TemplateRecipient;
import com.navisow.docusign.domain.template.TemplateVersion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/templates")
@Validated
public class TemplateController {

    private final DocuSignTemplateService templateService;

    public TemplateController(DocuSignTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody CreateTemplateRequest request) {
        DocuSignTemplate template = templateService.create(jwt, new CreateTemplateCommand(
            request.externalTemplateId(),
            request.name(),
            request.subject(),
            request.description(),
            request.definitionJson(),
            toRecipientCommands(request.recipients())));
        return ResponseEntity.ok(toMap(template));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        Page<DocuSignTemplate> templates = templateService.listForUser(jwt, PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of(
            "content", templates.getContent().stream().map(this::toMap).toList(),
            "page", templates.getNumber(),
            "size", templates.getSize(),
            "totalElements", templates.getTotalElements(),
            "totalPages", templates.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id) {
        TemplateDetails details = templateService.getDetailForUser(jwt, id);
        return ResponseEntity.ok(Map.of(
            "template", toMap(details.template()),
            "versions", details.versions().stream().map(this::toVersionMap).toList(),
            "recipients", details.recipients().stream().map(this::toRecipientMap).toList()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id,
        @RequestBody UpdateTemplateRequest request) {
        DocuSignTemplate template = templateService.update(jwt, id, new UpdateTemplateCommand(
            request.externalTemplateId(),
            request.name(),
            request.subject(),
            request.description(),
            request.definitionJson(),
            toRecipientCommands(request.recipients())));
        return ResponseEntity.ok(toMap(template));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sync(@AuthenticationPrincipal Jwt jwt) {
        List<DocuSignTemplate> synced = templateService.syncTemplatesFromDocuSign(jwt);
        return ResponseEntity.ok(Map.of(
            "count", synced.size(),
            "content", synced.stream().map(this::toMap).toList()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id) {
        templateService.delete(jwt, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(TemplateNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    private Map<String, Object> toMap(DocuSignTemplate template) {
        return Map.of(
            "id", template.getId(),
            "externalTemplateId", template.getExternalTemplateId(),
            "name", template.getName(),
            "subject", template.getSubject() == null ? "" : template.getSubject(),
            "description", template.getDescription() == null ? "" : template.getDescription(),
            "createdAt", template.getCreatedAt(),
            "updatedAt", template.getUpdatedAt());
    }

    private Map<String, Object> toVersionMap(TemplateVersion version) {
        return Map.of(
            "id", version.getId(),
            "versionNumber", version.getVersionNumber(),
            "definition", version.getDefinition() == null ? "" : version.getDefinition(),
            "active", version.isActive(),
            "createdAt", version.getCreatedAt());
    }

    private Map<String, Object> toRecipientMap(TemplateRecipient recipient) {
        return Map.of(
            "id", recipient.getId(),
            "roleId", recipient.getRoleId(),
            "recipientName", recipient.getRecipientName(),
            "recipientEmail", recipient.getRecipientEmail() == null ? "" : recipient.getRecipientEmail(),
            "recipientType", recipient.getRecipientType(),
            "sequenceOrder", recipient.getSequenceOrder());
    }

    private List<RecipientCommand> toRecipientCommands(List<TemplateRecipientRequest> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        return recipients.stream().map(r -> {
            Integer requestedOrder = r.sequenceOrder();
            int sequenceOrder = requestedOrder == null ? 1 : requestedOrder;
            return new RecipientCommand(
                r.roleId(),
                r.recipientName(),
                r.recipientEmail(),
                r.recipientType(),
                sequenceOrder);
        }).toList();
    }

    public record CreateTemplateRequest(
        @NotBlank String externalTemplateId,
        @NotBlank String name,
        String subject,
        String description,
        String definitionJson,
        List<@Valid TemplateRecipientRequest> recipients) {
    }

    public record UpdateTemplateRequest(
        @NotBlank String externalTemplateId,
        @NotBlank String name,
        String subject,
        String description,
        String definitionJson,
        List<@Valid TemplateRecipientRequest> recipients) {
    }

    public record TemplateRecipientRequest(
        @NotBlank String roleId,
        @NotBlank String recipientName,
        String recipientEmail,
        @NotBlank String recipientType,
        Integer sequenceOrder) {
    }
}
