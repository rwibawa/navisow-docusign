package com.navisow.docusign.domain.envelope;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnvelopeEventRepository extends JpaRepository<EnvelopeEvent, UUID> {
    List<EnvelopeEvent> findByEnvelope_IdOrderByOccurredAtDesc(UUID envelopeId);
}
