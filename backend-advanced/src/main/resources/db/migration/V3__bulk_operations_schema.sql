CREATE TABLE IF NOT EXISTS bulk_operation (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    total_count INTEGER NOT NULL DEFAULT 0,
    processed_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failure_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT fk_bulk_operation_user
        FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX IF NOT EXISTS idx_bulk_operation_user_id
    ON bulk_operation (user_id);

CREATE TABLE IF NOT EXISTS bulk_operation_item (
    id UUID PRIMARY KEY,
    bulk_operation_id UUID NOT NULL,
    document_id UUID,
    recipient_list JSONB,
    status VARCHAR(40) NOT NULL,
    envelope_id VARCHAR(120),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT fk_bulk_operation_item_bulk_operation
        FOREIGN KEY (bulk_operation_id) REFERENCES bulk_operation (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_bulk_operation_item_bulk_operation_id
    ON bulk_operation_item (bulk_operation_id);

CREATE INDEX IF NOT EXISTS idx_bulk_operation_item_envelope_id
    ON bulk_operation_item (envelope_id);
