package com.navisow.docusign.domain.webhook;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookProcessingRuleRepository extends JpaRepository<WebhookProcessingRule, UUID> {

    List<WebhookProcessingRule> findByEventTypeAndIsActiveTrue(String eventType);

    List<WebhookProcessingRule> findByUserIdAndIsActiveTrue(UUID userId);
}
