package com.navisow.docusign.domain.template;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRecipientRepository extends JpaRepository<TemplateRecipient, UUID> {

    List<TemplateRecipient> findByTemplateIdOrderBySequenceOrderAsc(UUID templateId);

    void deleteByTemplateId(UUID templateId);
}
