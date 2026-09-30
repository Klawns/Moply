-- Temporary operational data must be reset explicitly before applying V8.
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM tb_customer) OR EXISTS (SELECT 1 FROM tb_customer_location)
 OR EXISTS (SELECT 1 FROM tb_order_service) THEN
 RAISE EXCEPTION 'V8 requires empty operational tables. Reset the disposable database explicitly; no ownership can be inferred.';
 END IF;
END $$;
ALTER TABLE tb_customer ADD COLUMN organization_id UUID NOT NULL REFERENCES tb_organization(id);
ALTER TABLE tb_customer ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tb_customer ADD CONSTRAINT uk_customer_account UNIQUE (organization_id, id);
ALTER TABLE tb_customer_location ADD COLUMN organization_id UUID NOT NULL REFERENCES tb_organization(id);
ALTER TABLE tb_customer_location DROP CONSTRAINT fk_customer_location_customer;
ALTER TABLE tb_customer_location ADD CONSTRAINT fk_location_account_customer FOREIGN KEY (organization_id, customer_id) REFERENCES tb_customer(organization_id, id);
CREATE INDEX idx_location_account_customer ON tb_customer_location(organization_id, customer_id);
ALTER TABLE tb_order_service ADD COLUMN organization_id UUID NOT NULL REFERENCES tb_organization(id);
ALTER TABLE tb_order_service DROP CONSTRAINT fk_order_service_customer;
ALTER TABLE tb_order_service ADD CONSTRAINT fk_order_account_customer FOREIGN KEY (organization_id, customer_id) REFERENCES tb_customer(organization_id, id);
CREATE INDEX idx_order_account_date ON tb_order_service(organization_id, service_date);
CREATE INDEX idx_order_account_customer ON tb_order_service(organization_id, customer_id);
CREATE INDEX idx_customer_account_name ON tb_customer(organization_id, name);
