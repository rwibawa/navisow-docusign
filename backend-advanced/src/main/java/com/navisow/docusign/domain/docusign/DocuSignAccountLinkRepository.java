package com.navisow.docusign.domain.docusign;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocuSignAccountLinkRepository extends JpaRepository<DocuSignAccountLink, UUID> {

    Optional<DocuSignAccountLink> findByUser_IdAndAccountId(UUID userId, String accountId);

    Optional<DocuSignAccountLink> findFirstByUser_Id(UUID userId);
}
