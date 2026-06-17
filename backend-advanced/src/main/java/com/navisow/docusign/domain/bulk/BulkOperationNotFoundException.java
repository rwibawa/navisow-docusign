package com.navisow.docusign.domain.bulk;

import java.util.UUID;

public class BulkOperationNotFoundException extends RuntimeException {

    public BulkOperationNotFoundException(UUID operationId) {
        super("Bulk operation not found: " + operationId);
    }
}
