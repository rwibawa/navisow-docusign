CREATE TABLE IF NOT EXISTS analytics_snapshot (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    metric_type VARCHAR(80) NOT NULL,
    envelope_count INTEGER NOT NULL DEFAULT 0,
    signed_count INTEGER NOT NULL DEFAULT 0,
    pending_count INTEGER NOT NULL DEFAULT 0,
    average_time_to_sign_seconds BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_analytics_snapshot_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uk_analytics_snapshot UNIQUE (user_id, snapshot_date, metric_type)
);

CREATE INDEX IF NOT EXISTS idx_analytics_snapshot_user_date
    ON analytics_snapshot (user_id, snapshot_date);

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    user_id UUID,
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(120) NOT NULL,
    resource_id VARCHAR(120),
    old_value JSONB,
    new_value JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at
    ON audit_log (created_at);

CREATE INDEX IF NOT EXISTS idx_audit_log_resource
    ON audit_log (resource_type, resource_id);
