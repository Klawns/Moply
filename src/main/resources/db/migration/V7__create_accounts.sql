CREATE TABLE tb_organization (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    timezone TEXT NOT NULL,
    default_work_status VARCHAR(20) NOT NULL,
    CONSTRAINT chk_organization_name CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_organization_currency CHECK (currency_code = 'GBP'),
    CONSTRAINT chk_organization_timezone CHECK (length(trim(timezone)) > 0),
    CONSTRAINT chk_organization_default_status CHECK (default_work_status IN ('SCHEDULED', 'COMPLETED'))
);

CREATE TABLE tb_app_user (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    email TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    CONSTRAINT uk_app_user_organization UNIQUE (organization_id),
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT fk_app_user_organization FOREIGN KEY (organization_id) REFERENCES tb_organization (id),
    CONSTRAINT chk_app_user_email CHECK (email = lower(trim(email)) AND length(email) > 0),
    CONSTRAINT chk_app_user_password CHECK (length(password_hash) > 0)
);
