package com.navisow.docusign.domain.bulk;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulkOperationRepository extends JpaRepository<BulkOperation, UUID> {

    Page<BulkOperation> findByUserId(UUID userId, Pageable pageable);

    Optional<BulkOperation> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, BulkOperation.BulkOperationStatus status);

    List<BulkOperation> findByStatus(BulkOperation.BulkOperationStatus status);
}
