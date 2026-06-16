package com.navisow.docusign.api.webhook;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.navisow.docusign.domain.envelope.EnvelopeEvent;
import com.navisow.docusign.domain.envelope.EnvelopeEventRepository;
import com.navisow.docusign.domain.envelope.EnvelopeRecord;
import com.navisow.docusign.domain.envelope.EnvelopeRecord.EnvelopeStatus;
import com.navisow.docusign.domain.envelope.EnvelopeRecordRepository;

/**
 * Receives DocuSign Connect webhook notifications.
 *
 * Configuration in DocuSign Admin → Connect:
 *  - URL: https://your-domain/api/webhook/docusign
 *  - Trigger events: envelope-sent, envelope-delivered, envelope-completed,
 *                    envelope-declined, envelope-voided, recipient-completed
 *
 * HMAC signature validation should be enabled in production by configuring
 * {@code app.docusign.connect-hmac-key} and enabling the filter in SecurityConfig.
 */
@RestController
@RequestMapping("/api/webhook/docusign")
public class DocuSignWebhookController {

    private static final Logger log = LoggerFactory.getLogger(DocuSignWebhookController.class);

    private final EnvelopeRecordRepository envelopeRepository;
    private final EnvelopeEventRepository eventRepository;

    public DocuSignWebhookController(EnvelopeRecordRepository envelopeRepository,
                                      EnvelopeEventRepository eventRepository) {
        this.envelopeRepository = envelopeRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Accepts both JSON (DocuSign Connect REST v2) and XML payloads.
     * We accept JSON only here; XML support can be added via XmlMapper if required.
     */
    @PostMapping(consumes = {"application/json", "*/*"})
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> payload) {
        try {
            processPayload(payload);
        } catch (Exception ex) {
            // Log and acknowledge — retrying will not help for parse errors
            log.error("Failed to process DocuSign webhook payload", ex);
        }
        // Always return 200 to stop DocuSign from retrying
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private void processPayload(Map<String, Object> payload) {
        String event = (String) payload.get("event");

        Object dataObj = payload.get("data");
        if (!(dataObj instanceof Map<?, ?> data)) {
            log.warn("Webhook payload missing 'data' field, event={}", event);
            return;
        }

        Object envObj = ((Map<String, Object>) data).get("envelopeSummary");
        if (!(envObj instanceof Map<?, ?> envSummary)) {
            log.warn("Webhook payload missing envelopeSummary, event={}", event);
            return;
        }

        String dsEnvelopeId = (String) ((Map<String, Object>) envSummary).get("envelopeId");
        String rawStatus = (String) ((Map<String, Object>) envSummary).get("status");

        if (dsEnvelopeId == null) {
            log.warn("Webhook payload missing envelopeId, event={}", event);
            return;
        }

        Optional<EnvelopeRecord> opt = envelopeRepository.findByDocuSignEnvelopeId(dsEnvelopeId);
        if (opt.isEmpty()) {
            log.debug("Received webhook for unknown envelope {}, event={}", dsEnvelopeId, event);
            return;
        }

        EnvelopeRecord envelope = opt.get();

        // Map DocuSign status to our enum
        EnvelopeStatus newStatus = mapStatus(rawStatus);
        if (newStatus != null && !newStatus.equals(envelope.getStatus())) {
            envelope.setStatus(newStatus);
            envelopeRepository.save(envelope);
        }

        // Persist event for timeline
        EnvelopeEvent envelopeEvent = new EnvelopeEvent(
            envelope,
            event != null ? event : rawStatus,
            payload.toString(),  // simplified; parse full JSON string in production
            Instant.now()
        );
        eventRepository.save(envelopeEvent);

        log.info("Processed DocuSign webhook: envelopeId={}, event={}, newStatus={}",
            dsEnvelopeId, event, newStatus);
    }

    private EnvelopeStatus mapStatus(String dsStatus) {
        if (dsStatus == null) return null;
        return switch (dsStatus.toLowerCase()) {
            case "sent" -> EnvelopeStatus.SENT;
            case "delivered" -> EnvelopeStatus.DELIVERED;
            case "completed" -> EnvelopeStatus.COMPLETED;
            case "declined" -> EnvelopeStatus.DECLINED;
            case "voided" -> EnvelopeStatus.VOIDED;
            default -> null;
        };
    }
}
