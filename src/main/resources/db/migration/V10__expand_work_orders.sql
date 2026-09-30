-- Counts cannot identify historical participants. Abort without modifying data.
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM tb_order_service) THEN
  RAISE EXCEPTION 'V10 blocked: legacy work orders have no participant identities. Resolve incompatible data explicitly before retrying; no data was deleted.';
 END IF;
END $$;
ALTER TABLE tb_order_service
 ALTER COLUMN contracted_hours TYPE NUMERIC,
 ALTER COLUMN hourly_rate TYPE NUMERIC,
 ADD COLUMN customer_location_id UUID,
 ADD COLUMN start_time TIME,
 ADD COLUMN description TEXT,
 ADD COLUMN currency_code VARCHAR(3) NOT NULL,
 ADD COLUMN total_amount NUMERIC NOT NULL,
 ADD COLUMN allocation_policy_version INTEGER NOT NULL,
 ADD COLUMN status VARCHAR(20) NOT NULL,
 ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
 ADD CONSTRAINT uk_work_order_account UNIQUE (organization_id, id),
 ADD CONSTRAINT chk_work_hours CHECK (contracted_hours > 0 AND scale(contracted_hours) <= 2 AND contracted_hours < 'Infinity'::numeric),
 ADD CONSTRAINT chk_work_rate CHECK (hourly_rate > 0 AND scale(hourly_rate) <= 2 AND hourly_rate < 'Infinity'::numeric),
 ADD CONSTRAINT chk_work_total CHECK (total_amount > 0 AND scale(total_amount) <= 2 AND total_amount < 'Infinity'::numeric),
 ADD CONSTRAINT chk_work_currency CHECK (currency_code = 'GBP'),
 ADD CONSTRAINT chk_work_policy CHECK (allocation_policy_version > 0),
 ADD CONSTRAINT chk_work_version CHECK (version >= 0),
 ADD CONSTRAINT chk_work_status CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED'));
ALTER TABLE tb_customer_location ADD CONSTRAINT uk_location_account_customer UNIQUE (organization_id, customer_id, id);
ALTER TABLE tb_order_service ADD CONSTRAINT fk_work_location_customer
 FOREIGN KEY (organization_id, customer_id, customer_location_id) REFERENCES tb_customer_location(organization_id, customer_id, id);
CREATE TABLE tb_work_assignment (
 id UUID PRIMARY KEY,
 organization_id UUID NOT NULL,
 work_order_id UUID NOT NULL,
 collaborator_id UUID NOT NULL,
 inclusion_position INTEGER NOT NULL CHECK (inclusion_position >= 0),
 allocated_amount NUMERIC NOT NULL CHECK (allocated_amount >= 0 AND scale(allocated_amount) <= 2 AND allocated_amount < 'Infinity'::numeric),
 CONSTRAINT uk_assignment_collaborator UNIQUE (work_order_id, collaborator_id),
 CONSTRAINT uk_assignment_position UNIQUE (work_order_id, inclusion_position),
 CONSTRAINT fk_assignment_work FOREIGN KEY (organization_id, work_order_id) REFERENCES tb_order_service(organization_id,id),
 CONSTRAINT fk_assignment_collaborator FOREIGN KEY (organization_id, collaborator_id) REFERENCES tb_collaborator(organization_id,id)
);
CREATE INDEX idx_assignment_account_collaborator ON tb_work_assignment(organization_id,collaborator_id);
