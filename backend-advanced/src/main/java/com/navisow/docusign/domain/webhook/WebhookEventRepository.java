package com.navisow.docusign.domain.webhook;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Optional<WebhookEvent> findByExternalEventId(String externalEventId);

    java.util.List<WebhookEvent> findByStatusOrderByCreatedAtAsc(WebhookEvent.WebhookEventStatus status);

    Page<WebhookEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
