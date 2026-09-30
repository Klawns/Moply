DO $$ BEGIN
 IF EXISTS (
  SELECT w.id FROM tb_order_service w LEFT JOIN tb_work_assignment a ON a.work_order_id = w.id
  GROUP BY w.id, w.total_amount
  HAVING count(a.id) = 0 OR sum(a.allocated_amount) <> w.total_amount
   OR min(a.inclusion_position) <> 0 OR max(a.inclusion_position) <> count(a.id) - 1
 ) THEN
  RAISE EXCEPTION 'V11 blocked: invalid work assignments. Every work requires ordered participants conserving the persisted total.';
 END IF;
END $$;
ALTER TABLE tb_order_service DROP COLUMN employee_count;
