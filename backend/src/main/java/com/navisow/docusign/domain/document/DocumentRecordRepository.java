package com.navisow.docusign.domain.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, UUID> {
    Page<DocumentRecord> findByUser_Id(UUID userId, Pageable pageable);
}
