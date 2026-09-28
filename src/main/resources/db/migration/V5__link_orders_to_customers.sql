-- Preserve historical names and never merge customers by name.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM tb_order_service WHERE customer IS NULL OR customer !~ '[^[:space:]]') THEN
        RAISE EXCEPTION 'Cannot link orders: invalid historical customer names. Correct the names before retrying.';
    END IF;
END $$;

ALTER TABLE tb_order_service ALTER COLUMN customer TYPE TEXT;
ALTER TABLE tb_order_service ADD COLUMN customer_id UUID;
UPDATE tb_order_service SET customer_id = gen_random_uuid();
INSERT INTO tb_customer (id, name)
SELECT customer_id, regexp_replace(customer, '^[[:space:]]+|[[:space:]]+$', '', 'g')
FROM tb_order_service;
ALTER TABLE tb_order_service ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE tb_order_service ADD CONSTRAINT fk_order_service_customer
    FOREIGN KEY (customer_id) REFERENCES tb_customer (id);
CREATE INDEX idx_order_service_customer_id ON tb_order_service (customer_id);
