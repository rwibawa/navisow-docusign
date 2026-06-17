CREATE TABLE IF NOT EXISTS webhook_event (
    id UUID PRIMARY KEY,
    external_event_id VARCHAR(120) NOT NULL,
    envelope_id VARCHAR(120),
    event_type VARCHAR(80) NOT NULL,
    payload JSONB,
    status VARCHAR(40) NOT NULL,
    error_message TEXT,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_webhook_event_external UNIQUE (external_event_id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_event_created_at
    ON webhook_event (created_at);

CREATE INDEX IF NOT EXISTS idx_webhook_event_envelope
    ON webhook_event (envelope_id);

CREATE TABLE IF NOT EXISTS webhook_processing_rule (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    action VARCHAR(120) NOT NULL,
    target_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_webhook_rule_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_rule_user
    ON webhook_processing_rule (user_id);
