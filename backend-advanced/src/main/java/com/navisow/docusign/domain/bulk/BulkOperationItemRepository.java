package com.navisow.docusign.domain.bulk;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BulkOperationItemRepository extends JpaRepository<BulkOperationItem, UUID> {

    List<BulkOperationItem> findByBulkOperationId(UUID bulkOperationId);

    List<BulkOperationItem> findByBulkOperationIdAndStatusNot(UUID bulkOperationId, BulkOperationItem.BulkItemStatus status);
}
