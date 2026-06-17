package com.navisow.docusign.api.webhook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navisow.docusign.integration.docusign.DocuSignWebhookSignatureService;
import com.navisow.docusign.domain.webhook.WebhookEvent;
import com.navisow.docusign.domain.webhook.WebhookEventService;
import com.navisow.docusign.domain.webhook.WebhookEventService.CreateRuleCommand;
import com.navisow.docusign.domain.webhook.WebhookProcessingRule;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook")
@Validated
public class AdvancedWebhookController {

    private final WebhookEventService webhookEventService;
    private final DocuSignWebhookSignatureService signatureService;
    private final ObjectMapper objectMapper;

    public AdvancedWebhookController(
        WebhookEventService webhookEventService,
        DocuSignWebhookSignatureService signatureService,
        ObjectMapper objectMapper) {
        this.webhookEventService = webhookEventService;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> receive(
        @RequestBody String payloadJson,
        @RequestHeader(value = "X-DocuSign-Signature-1", required = false) String signature) {
        if (!signatureService.verify(payloadJson, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid webhook signature"));
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid JSON payload"));
        }

        WebhookEvent event = webhookEventService.processIncomingEvent(payload, payloadJson);
        return ResponseEntity.ok(Map.of(
            "id", event.getId(),
            "externalEventId", event.getExternalEventId(),
            "status", event.getStatus().name()));
    }

    @PostMapping("/events/retry-failed")
    public ResponseEntity<Map<String, Object>> retryFailedEvents() {
        int retried = webhookEventService.retryFailedEvents();
        return ResponseEntity.ok(Map.of("retried", retried));
    }

    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> listEvents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        Page<WebhookEvent> events = webhookEventService.listEvents(PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of(
            "content", events.getContent().stream().map(this::toEventMap).toList(),
            "page", events.getNumber(),
            "size", events.getSize(),
            "totalElements", events.getTotalElements(),
            "totalPages", events.getTotalPages()));
    }

    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> createRule(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody CreateRuleRequest request) {
        WebhookProcessingRule rule = webhookEventService.createRule(jwt, new CreateRuleCommand(
            request.eventType(),
            request.action(),
            request.targetUrl(),
            request.active() == null || request.active()));
        return ResponseEntity.ok(toRuleMap(rule));
    }

    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> listRules(@AuthenticationPrincipal Jwt jwt) {
        List<WebhookProcessingRule> rules = webhookEventService.listActiveRulesForUser(jwt);
        return ResponseEntity.ok(Map.of("content", rules.stream().map(this::toRuleMap).toList()));
    }

    private Map<String, Object> toEventMap(WebhookEvent event) {
        return Map.of(
            "id", event.getId(),
            "externalEventId", event.getExternalEventId(),
            "envelopeId", event.getEnvelopeId() == null ? "" : event.getEnvelopeId(),
            "eventType", event.getEventType(),
            "status", event.getStatus().name(),
            "processedAt", event.getProcessedAt() == null ? "" : event.getProcessedAt().toString(),
            "createdAt", event.getCreatedAt());
    }

    private Map<String, Object> toRuleMap(WebhookProcessingRule rule) {
        return Map.of(
            "id", rule.getId(),
            "eventType", rule.getEventType(),
            "action", rule.getAction(),
            "targetUrl", rule.getTargetUrl() == null ? "" : rule.getTargetUrl(),
            "active", rule.isActive(),
            "createdAt", rule.getCreatedAt(),
            "updatedAt", rule.getUpdatedAt());
    }

    public record CreateRuleRequest(
        @NotBlank String eventType,
        @NotBlank String action,
        String targetUrl,
        Boolean active) {
    }
}
