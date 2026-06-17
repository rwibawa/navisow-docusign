package com.navisow.docusign.domain.template;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, UUID> {

    List<TemplateVersion> findByTemplateIdOrderByVersionNumberDesc(UUID templateId);

    Optional<TemplateVersion> findTopByTemplateIdOrderByVersionNumberDesc(UUID templateId);

    List<TemplateVersion> findByTemplateIdAndIsActiveTrue(UUID templateId);
}
