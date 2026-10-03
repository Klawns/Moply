CREATE TABLE tb_customer_payment (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    work_order_id UUID NOT NULL,
    amount NUMERIC NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    paid_on DATE NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    recorded_by UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    reversed_at TIMESTAMPTZ,
    reversed_by UUID,
    reversal_reason TEXT,
    CONSTRAINT fk_payment_work_order FOREIGN KEY (organization_id, work_order_id)
        REFERENCES tb_order_service (organization_id, id),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_currency CHECK (currency_code = 'GBP'),
    CONSTRAINT chk_payment_status CHECK (status IN ('RECORDED', 'REVERSED')),
    CONSTRAINT chk_payment_reversal_audit CHECK (
        (status = 'RECORDED' AND reversed_at IS NULL AND reversed_by IS NULL AND reversal_reason IS NULL)
        OR (status = 'REVERSED' AND reversed_at IS NOT NULL AND reversed_by IS NOT NULL
            AND length(trim(reversal_reason)) > 0)
    )
);

CREATE UNIQUE INDEX uk_payment_active_work_order ON tb_customer_payment (organization_id, work_order_id)
    WHERE status = 'RECORDED';
CREATE INDEX idx_payment_account_work ON tb_customer_payment (organization_id, work_order_id, recorded_at);
