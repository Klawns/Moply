CREATE TABLE tb_recurrence_series (
 id UUID PRIMARY KEY,
 organization_id UUID NOT NULL REFERENCES tb_organization(id),
 frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('WEEKLY','BIWEEKLY','MONTHLY')),
 starts_on DATE NOT NULL,
 ends_on DATE,
 customer_id UUID NOT NULL,
 customer_location_id UUID,
 start_time TIME,
 description TEXT,
 contracted_hours NUMERIC NOT NULL CHECK (contracted_hours > 0 AND scale(contracted_hours) <= 2 AND contracted_hours < 'Infinity'::numeric),
 hourly_rate NUMERIC NOT NULL CHECK (hourly_rate > 0 AND scale(hourly_rate) <= 2 AND hourly_rate < 'Infinity'::numeric),
 currency_code VARCHAR(3) NOT NULL CHECK (currency_code = 'GBP'),
 initial_status VARCHAR(20) NOT NULL CHECK (initial_status IN ('SCHEDULED','COMPLETED')),
 version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
 CONSTRAINT chk_series_dates CHECK (ends_on IS NULL OR ends_on >= starts_on),
 CONSTRAINT uk_series_account UNIQUE (organization_id,id),
 CONSTRAINT fk_series_customer FOREIGN KEY (organization_id,customer_id) REFERENCES tb_customer(organization_id,id),
 CONSTRAINT fk_series_location FOREIGN KEY (organization_id,customer_id,customer_location_id) REFERENCES tb_customer_location(organization_id,customer_id,id)
);
CREATE TABLE tb_recurrence_member (
 id UUID PRIMARY KEY,
 organization_id UUID NOT NULL,
 series_id UUID NOT NULL,
 collaborator_id UUID NOT NULL,
 inclusion_position INTEGER NOT NULL CHECK (inclusion_position >= 0),
 CONSTRAINT uk_series_member UNIQUE (series_id,collaborator_id),
 CONSTRAINT uk_series_position UNIQUE (series_id,inclusion_position),
 CONSTRAINT fk_member_series FOREIGN KEY (organization_id,series_id) REFERENCES tb_recurrence_series(organization_id,id),
 CONSTRAINT fk_member_collaborator FOREIGN KEY (organization_id,collaborator_id) REFERENCES tb_collaborator(organization_id,id)
);
ALTER TABLE tb_order_service
 ADD COLUMN recurrence_series_id UUID,
 ADD COLUMN occurrence_date DATE,
 ADD CONSTRAINT chk_work_occurrence CHECK ((recurrence_series_id IS NULL) = (occurrence_date IS NULL)),
 ADD CONSTRAINT fk_work_series FOREIGN KEY (organization_id,recurrence_series_id) REFERENCES tb_recurrence_series(organization_id,id),
 ADD CONSTRAINT uk_work_series_occurrence UNIQUE (recurrence_series_id,occurrence_date);
