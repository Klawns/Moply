CREATE TABLE tb_revoked_token (
    id UUID PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_revoked_token_expiry ON tb_revoked_token (expires_at);

CREATE TABLE tb_auth_rate_guard (id INTEGER PRIMARY KEY);
INSERT INTO tb_auth_rate_guard (id) VALUES (1);
CREATE TABLE tb_auth_rate_bucket (
    id VARCHAR(80) PRIMARY KEY,
    tokens DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_auth_rate_bucket_expiry ON tb_auth_rate_bucket (expires_at);
