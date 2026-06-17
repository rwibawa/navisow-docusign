package com.navisow.docusign.domain.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.domain.user.AppUserService;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class WebhookEventService {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookProcessingRuleRepository ruleRepository;
    private final AppUserService appUserService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public WebhookEventService(
        WebhookEventRepository webhookEventRepository,
        WebhookProcessingRuleRepository ruleRepository,
        AppUserService appUserService,
        ObjectMapper objectMapper,
        RestClient.Builder builder) {
        this.webhookEventRepository = webhookEventRepository;
        this.ruleRepository = ruleRepository;
        this.appUserService = appUserService;
        this.objectMapper = objectMapper;
        this.restClient = builder.build();
    }

    @Transactional
    public WebhookEvent processIncomingEvent(Map<String, Object> payload, String rawPayload) {
        String incomingEventId = String.valueOf(payload.getOrDefault("eventId", ""));
        if (incomingEventId.isBlank()) {
            incomingEventId = "event-" + System.currentTimeMillis();
        }
        final String externalEventId = incomingEventId;

        return webhookEventRepository.findByExternalEventId(externalEventId)
            .orElseGet(() -> {
                String envelopeId = String.valueOf(payload.getOrDefault("envelopeId", ""));
                String eventType = String.valueOf(payload.getOrDefault("event", "UNKNOWN"));
                WebhookEvent event = new WebhookEvent(externalEventId, envelopeId, eventType, rawPayload);
                WebhookEvent saved = webhookEventRepository.save(event);
                return processStoredEvent(saved, payload);
            });
    }

    @Transactional
    public int retryFailedEvents() {
        List<WebhookEvent> failedEvents = webhookEventRepository
            .findByStatusOrderByCreatedAtAsc(WebhookEvent.WebhookEventStatus.FAILED);

        int retried = 0;
        for (WebhookEvent event : failedEvents) {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() {
                });
                processStoredEvent(event, payload);
                retried++;
            } catch (JsonProcessingException | IllegalStateException | RestClientException ex) {
                event.markFailed(ex.getMessage());
                webhookEventRepository.save(event);
            }
        }
        return retried;
    }

    @Transactional(readOnly = true)
    public Page<WebhookEvent> listEvents(Pageable pageable) {
        return webhookEventRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public WebhookProcessingRule createRule(Jwt jwt, CreateRuleCommand command) {
        AppUser user = appUserService.getOrCreateUser(jwt);
        WebhookProcessingRule rule = new WebhookProcessingRule(
            user,
            command.eventType(),
            command.action(),
            command.targetUrl(),
            command.active());
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<WebhookProcessingRule> listActiveRulesForUser(Jwt jwt) {
        AppUser user = appUserService.getOrCreateUser(jwt);
        return ruleRepository.findByUserIdAndIsActiveTrue(user.getId());
    }

    private WebhookEvent processStoredEvent(WebhookEvent event, Map<String, Object> payload) {
        try {
            applyProcessingRules(event.getEventType(), payload);
            event.markProcessed();
        } catch (IllegalStateException | RestClientException ex) {
            event.markFailed(ex.getMessage());
        }
        return webhookEventRepository.save(event);
    }

    private void applyProcessingRules(String eventType, Map<String, Object> payload) {
        List<WebhookProcessingRule> rules = ruleRepository.findByEventTypeAndIsActiveTrue(eventType);
        for (WebhookProcessingRule rule : rules) {
            executeRule(rule, payload);
        }
    }

    private void executeRule(WebhookProcessingRule rule, Map<String, Object> payload) {
        String action = rule.getAction();
        if (action == null || action.isBlank() || "LOG_ONLY".equalsIgnoreCase(action)) {
            return;
        }
        if ("FORWARD_HTTP_POST".equalsIgnoreCase(action)) {
            String targetUrl = rule.getTargetUrl();
            if (targetUrl == null || targetUrl.isBlank()) {
                throw new IllegalStateException("FORWARD_HTTP_POST rule requires targetUrl");
            }
            restClient.post()
                .uri(targetUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
            return;
        }
        throw new IllegalStateException("Unsupported webhook rule action: " + action);
    }

    public record CreateRuleCommand(String eventType, String action, String targetUrl, boolean active) {
    }
}
