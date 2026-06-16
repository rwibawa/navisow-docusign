package com.navisow.docusign.domain.envelope;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnvelopeRecordRepository extends JpaRepository<EnvelopeRecord, UUID> {
    Page<EnvelopeRecord> findByUser_Id(UUID userId, Pageable pageable);
    Optional<EnvelopeRecord> findByDocuSignEnvelopeId(String docuSignEnvelopeId);
}
