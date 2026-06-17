package com.navisow.docusign.domain.template;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocuSignTemplateRepository extends JpaRepository<DocuSignTemplate, UUID> {

    Page<DocuSignTemplate> findByUserId(UUID userId, Pageable pageable);

    Optional<DocuSignTemplate> findByIdAndUserId(UUID id, UUID userId);

    Optional<DocuSignTemplate> findByUserIdAndExternalTemplateId(UUID userId, String externalTemplateId);

    long countByUserId(UUID userId);
}
