-- Additive migration: historical orders and series retain their original allocations.
ALTER TABLE tb_collaborator
 ADD COLUMN hourly_rate NUMERIC,
 ADD CONSTRAINT chk_collaborator_hourly_rate CHECK (
  hourly_rate IS NULL OR (hourly_rate > 0 AND scale(hourly_rate) <= 2 AND hourly_rate < 'Infinity'::numeric)
 );
ALTER TABLE tb_organization
 ADD COLUMN default_hourly_rate NUMERIC,
 ADD CONSTRAINT chk_organization_default_hourly_rate CHECK (
  default_hourly_rate IS NULL OR (default_hourly_rate > 0 AND scale(default_hourly_rate) <= 2 AND default_hourly_rate < 'Infinity'::numeric)
 );
ALTER TABLE tb_recurrence_series
 ADD COLUMN total_amount NUMERIC,
 ADD COLUMN allocation_policy_version INTEGER,
 ADD CONSTRAINT chk_series_frozen_pricing CHECK (
  (total_amount IS NULL AND allocation_policy_version IS NULL) OR
  (total_amount IS NOT NULL AND allocation_policy_version IS NOT NULL AND
   total_amount > 0 AND scale(total_amount) <= 2 AND total_amount < 'Infinity'::numeric AND allocation_policy_version > 0)
 );
ALTER TABLE tb_work_assignment
 ADD COLUMN applied_hourly_rate NUMERIC,
 ADD COLUMN fixed_rate BOOLEAN,
 ADD COLUMN base_amount NUMERIC,
 ADD COLUMN surplus_amount NUMERIC,
 ADD CONSTRAINT chk_assignment_pricing_snapshot CHECK (
  (applied_hourly_rate IS NULL AND fixed_rate IS NULL AND base_amount IS NULL AND surplus_amount IS NULL) OR
  (applied_hourly_rate IS NOT NULL AND fixed_rate IS NOT NULL AND base_amount IS NOT NULL AND surplus_amount IS NOT NULL AND allocated_amount IS NOT NULL AND
   applied_hourly_rate > 0 AND scale(applied_hourly_rate) <= 2 AND applied_hourly_rate < 'Infinity'::numeric AND
   base_amount >= 0 AND scale(base_amount) <= 2 AND base_amount < 'Infinity'::numeric AND
   surplus_amount >= 0 AND scale(surplus_amount) <= 2 AND surplus_amount < 'Infinity'::numeric AND
   base_amount + surplus_amount = allocated_amount)
 );
ALTER TABLE tb_recurrence_member
 ADD COLUMN allocated_amount NUMERIC,
 ADD CONSTRAINT chk_member_allocated_amount CHECK (allocated_amount IS NULL OR (allocated_amount >= 0 AND scale(allocated_amount) <= 2 AND allocated_amount < 'Infinity'::numeric)),
 ADD COLUMN applied_hourly_rate NUMERIC,
 ADD COLUMN fixed_rate BOOLEAN,
 ADD COLUMN base_amount NUMERIC,
 ADD COLUMN surplus_amount NUMERIC,
 ADD CONSTRAINT chk_member_pricing_snapshot CHECK (
  (applied_hourly_rate IS NULL AND fixed_rate IS NULL AND base_amount IS NULL AND surplus_amount IS NULL) OR
  (applied_hourly_rate IS NOT NULL AND fixed_rate IS NOT NULL AND base_amount IS NOT NULL AND surplus_amount IS NOT NULL AND allocated_amount IS NOT NULL AND
   applied_hourly_rate > 0 AND scale(applied_hourly_rate) <= 2 AND applied_hourly_rate < 'Infinity'::numeric AND
   base_amount >= 0 AND scale(base_amount) <= 2 AND base_amount < 'Infinity'::numeric AND
   surplus_amount >= 0 AND scale(surplus_amount) <= 2 AND surplus_amount < 'Infinity'::numeric AND
   base_amount + surplus_amount = allocated_amount)
 );
