CREATE TABLE IF NOT EXISTS docusign_template (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    external_template_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_docusign_template_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uk_docusign_template_external_id UNIQUE (external_template_id)
);

CREATE INDEX IF NOT EXISTS idx_docusign_template_user_id
    ON docusign_template (user_id);

CREATE TABLE IF NOT EXISTS template_version (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    definition JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_template_version_template
        FOREIGN KEY (template_id) REFERENCES docusign_template (id) ON DELETE CASCADE,
    CONSTRAINT uk_template_version UNIQUE (template_id, version_number)
);

CREATE TABLE IF NOT EXISTS template_recipient (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    role_id VARCHAR(120) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(320),
    recipient_type VARCHAR(60) NOT NULL,
    sequence_order INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_template_recipient_template
        FOREIGN KEY (template_id) REFERENCES docusign_template (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_template_recipient_template_id
    ON template_recipient (template_id);
