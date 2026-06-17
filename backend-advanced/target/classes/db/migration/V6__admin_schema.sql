CREATE TABLE IF NOT EXISTS user_admin_state (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    suspended BOOLEAN NOT NULL DEFAULT FALSE,
    suspension_reason VARCHAR(500),
    suspended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_admin_state_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_user_admin_state_suspended
    ON user_admin_state (suspended);
