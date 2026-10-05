ALTER TABLE tb_recurrence_series
 ADD COLUMN family_id UUID,
 ADD COLUMN previous_series_id UUID,
 ADD COLUMN first_position BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN until_position BIGINT;
UPDATE tb_recurrence_series SET family_id=id;
ALTER TABLE tb_recurrence_series
 ALTER COLUMN family_id SET NOT NULL,
 ADD CONSTRAINT uk_series_family_member UNIQUE (organization_id,family_id,id),
 ADD CONSTRAINT fk_series_family FOREIGN KEY (organization_id,family_id) REFERENCES tb_recurrence_series(organization_id,id),
 ADD CONSTRAINT fk_series_previous FOREIGN KEY (organization_id,family_id,previous_series_id) REFERENCES tb_recurrence_series(organization_id,family_id,id),
 ADD CONSTRAINT chk_series_lineage CHECK (previous_series_id IS NULL OR previous_series_id <> id),
 ADD CONSTRAINT chk_series_positions CHECK (first_position >= 0 AND (until_position IS NULL OR until_position >= first_position));
CREATE INDEX ix_series_family ON tb_recurrence_series(organization_id,family_id);

CREATE TABLE tb_recurrence_command (
 id UUID PRIMARY KEY,
 organization_id UUID NOT NULL,
 family_id UUID NOT NULL,
 command_key VARCHAR(255) NOT NULL,
 content TEXT NOT NULL,
 actor_id UUID NOT NULL,
 recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
 successor_id UUID,
 CONSTRAINT uk_recurrence_command UNIQUE (organization_id,family_id,command_key),
 CONSTRAINT uk_recurrence_command_account UNIQUE (organization_id,id),
 CONSTRAINT fk_command_family FOREIGN KEY (organization_id,family_id) REFERENCES tb_recurrence_series(organization_id,id),
 CONSTRAINT fk_command_successor FOREIGN KEY (organization_id,family_id,successor_id) REFERENCES tb_recurrence_series(organization_id,family_id,id)
);
CREATE TABLE tb_recurrence_change_item (
 id UUID PRIMARY KEY,
 organization_id UUID NOT NULL,
 command_id UUID NOT NULL,
 work_id UUID NOT NULL,
 position BIGINT NOT NULL CHECK (position >= 0),
 reason VARCHAR(255) NOT NULL,
 target_series_id UUID,
 service_date_before DATE NOT NULL,
 service_date_after DATE NOT NULL,
 CONSTRAINT uk_change_work UNIQUE (command_id,work_id),
 CONSTRAINT fk_change_command FOREIGN KEY (organization_id,command_id) REFERENCES tb_recurrence_command(organization_id,id),
 CONSTRAINT fk_change_work FOREIGN KEY (organization_id,work_id) REFERENCES tb_order_service(organization_id,id),
 CONSTRAINT fk_change_target FOREIGN KEY (organization_id,target_series_id) REFERENCES tb_recurrence_series(organization_id,id)
);
CREATE INDEX ix_change_work ON tb_recurrence_change_item(organization_id,work_id);
CREATE TABLE tb_recurrence_exclusion (
 id UUID PRIMARY KEY,
 organization_id UUID NOT NULL,
 family_id UUID NOT NULL,
 position BIGINT NOT NULL CHECK (position >= 0),
 work_id UUID NOT NULL,
 CONSTRAINT uk_recurrence_exclusion UNIQUE (organization_id,family_id,position),
 CONSTRAINT fk_exclusion_family FOREIGN KEY (organization_id,family_id) REFERENCES tb_recurrence_series(organization_id,id),
 CONSTRAINT fk_exclusion_work FOREIGN KEY (organization_id,work_id) REFERENCES tb_order_service(organization_id,id)
);
