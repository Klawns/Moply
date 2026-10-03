ALTER TABLE tb_work_assignment
    ADD CONSTRAINT uk_assignment_account_work_collaborator
        UNIQUE (organization_id, work_order_id, collaborator_id);

CREATE TABLE tb_collaborator_payment (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    work_order_id UUID NOT NULL,
    collaborator_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    amount NUMERIC NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    paid_on DATE NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    recorded_by UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    reversed_at TIMESTAMPTZ,
    reversed_by UUID,
    reversal_reason TEXT,
    CONSTRAINT uk_collaborator_payment_idempotency UNIQUE (organization_id, idempotency_key),
    CONSTRAINT fk_collaborator_payment_assignment
        FOREIGN KEY (organization_id, work_order_id, collaborator_id)
        REFERENCES tb_work_assignment (organization_id, work_order_id, collaborator_id),
    CONSTRAINT chk_collaborator_payment_amount
        CHECK (amount > 0 AND scale(amount) <= 2 AND amount < 'Infinity'::numeric),
    CONSTRAINT chk_collaborator_payment_currency CHECK (currency_code = 'GBP'),
    CONSTRAINT chk_collaborator_payment_status CHECK (status IN ('RECORDED', 'REVERSED')),
    CONSTRAINT chk_collaborator_payment_key CHECK (length(trim(idempotency_key)) > 0),
    CONSTRAINT chk_collaborator_payment_reversal_audit CHECK (
        (status = 'RECORDED' AND reversed_at IS NULL AND reversed_by IS NULL AND reversal_reason IS NULL)
        OR (status = 'REVERSED' AND reversed_at IS NOT NULL AND reversed_by IS NOT NULL
            AND length(trim(reversal_reason)) > 0)
    )
);

CREATE INDEX idx_collaborator_payment_work_assignment
    ON tb_collaborator_payment (organization_id, work_order_id, collaborator_id, recorded_at);

CREATE INDEX idx_collaborator_payment_collaborator
    ON tb_collaborator_payment (organization_id, collaborator_id, work_order_id)
    WHERE status = 'RECORDED';
