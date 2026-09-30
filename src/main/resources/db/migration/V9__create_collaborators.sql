CREATE TABLE tb_collaborator (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES tb_organization(id),
    name TEXT NOT NULL,
    phone TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_collaborator_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT uk_collaborator_account UNIQUE (organization_id, id)
);

CREATE INDEX idx_collaborator_account_active ON tb_collaborator(organization_id, active);
