-- Contract Templates table
CREATE TABLE contract_templates (
    template_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    template_name VARCHAR(255) NOT NULL,
    template_content TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Index for faster lookup by owner
CREATE INDEX idx_contract_templates_owner ON contract_templates(owner_id);
