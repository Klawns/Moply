package com.klaus.moply.recurrence.infra;

import java.util.*;
import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import com.klaus.moply.factory.PostgresTestDatabase;

class RecurrenceMigrationTest {

	DriverManagerDataSource source;

	JdbcTemplate jdbc;

	String schema;

	@BeforeEach
	void setup() {
		schema = "work_" + UUID.randomUUID().toString().replace("-", "");
		var postgres = PostgresTestDatabase.POSTGRES;
		jdbc = new JdbcTemplate(
				new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
		jdbc.execute("CREATE SCHEMA " + schema);
		source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
		var props = new Properties();
		props.setProperty("currentSchema", schema);
		source.setConnectionProperties(props);
		jdbc = new JdbcTemplate(source);
	}

	@AfterEach
	void cleanup() {
		jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
	}

	Flyway flyway(String version) {
		return Flyway.configure().dataSource(source).schemas(schema).target(version).load();
	}

	UUID account() {
		UUID id = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO tb_organization(id,name,currency_code,timezone,default_work_status) VALUES (?,'Account','GBP','UTC','SCHEDULED')",
				id);
		return id;
	}

	UUID customer(UUID account) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO tb_customer(id,organization_id,name) VALUES (?,?,'Customer')", id, account);
		return id;
	}

	UUID person(UUID account) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO tb_collaborator(id,organization_id,name) VALUES (?,?,'Worker')", id, account);
		return id;
	}

	UUID work(UUID account, UUID customer, UUID location) {
		UUID id = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO tb_order_service(id,organization_id,customer_id,customer_location_id,service_date,contracted_hours,hourly_rate,currency_code,total_amount,allocation_policy_version,status) VALUES (?,?,?,?,CURRENT_DATE,1,10,'GBP',10,1,'SCHEDULED')",
				id, account, customer, location);
		return id;
	}

	void assignment(UUID account, UUID work, UUID person, int position, String amount) {
		jdbc.update(
				"INSERT INTO tb_work_assignment(id,organization_id,work_order_id,collaborator_id,inclusion_position,allocated_amount) VALUES (?,?,?,?,?,?)",
				UUID.randomUUID(), account, work, person, position, new BigDecimal(amount));
	}

	UUID series(UUID account, UUID customer) {
		var id = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO tb_recurrence_series(id,organization_id,frequency,starts_on,customer_id,contracted_hours,hourly_rate,currency_code,initial_status) VALUES (?,?,'WEEKLY','2026-10-03',?,1,10,'GBP','SCHEDULED')",
				id, account, customer);
		return id;
	}

	void member(UUID account, UUID series, UUID person, int position) {
		jdbc.update(
				"INSERT INTO tb_recurrence_member(id,organization_id,series_id,collaborator_id,inclusion_position) VALUES (?,?,?,?,?)",
				UUID.randomUUID(), account, series, person, position);
	}

	@Test
	void shouldMigrateFromV13PreservingWorksAndMultiplePartialSettlements() {
		assertEquals(13, flyway("13").migrate().migrationsExecuted);
		var a = account();
		var c = customer(a);
		var p = person(a);
		var w = work(a, c, null);
		assignment(a, w, p, 0, "10.00");
		for (int i = 0; i < 2; i++)
			jdbc.update(
					"INSERT INTO tb_collaborator_payment(id,organization_id,work_order_id,collaborator_id,idempotency_key,amount,currency_code,paid_on,recorded_at,recorded_by,status) VALUES (?,?,?,?,?,2,'GBP',CURRENT_DATE,CURRENT_TIMESTAMP,?,'RECORDED')",
					UUID.randomUUID(), a, w, p, "partial-" + i, UUID.randomUUID());
		assertEquals(1, flyway("14").migrate().migrationsExecuted);
		flyway("14").validate();
		assertEquals(0, flyway("14").migrate().migrationsExecuted);
		assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM tb_collaborator_payment", Integer.class));
		assertEquals(1, jdbc.queryForObject(
				"SELECT count(*) FROM tb_order_service WHERE recurrence_series_id IS NULL AND occurrence_date IS NULL",
				Integer.class));
		assertEquals(0, new BigDecimal("4.00")
			.compareTo(jdbc.queryForObject("SELECT sum(amount) FROM tb_collaborator_payment", BigDecimal.class)));
	}

	@Test
	void shouldEnforceTenantLinksPositionsAndOriginalDateUniquenessInPostgres() {
		assertEquals(14, flyway("14").migrate().migrationsExecuted);
		var a = account();
		var b = account();
		var ca = customer(a);
		var cb = customer(b);
		var pa = person(a);
		var pb = person(b);
		var sa = series(a, ca);
		var sb = series(b, cb);
		member(a, sa, pa, 0);
		assertThrows(DataIntegrityViolationException.class, () -> series(a, cb));
		assertThrows(DataIntegrityViolationException.class, () -> member(a, sa, pb, 1));
		assertThrows(DataIntegrityViolationException.class, () -> member(a, sb, pa, 1));
		assertThrows(DataIntegrityViolationException.class, () -> member(a, sa, pa, 1));
		assertThrows(DataIntegrityViolationException.class, () -> member(a, sa, person(a), 0));
		var w = work(a, ca, null);
		var other = work(a, ca, null);
		jdbc.update("UPDATE tb_order_service SET recurrence_series_id=?, occurrence_date='2026-10-03' WHERE id=?", sa,
				w);
		jdbc.update("UPDATE tb_order_service SET service_date='2026-12-01', status='CANCELLED' WHERE id=?", w);
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update(
						"UPDATE tb_order_service SET recurrence_series_id=?, occurrence_date='2026-10-03' WHERE id=?",
						sa, other));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update(
						"UPDATE tb_order_service SET recurrence_series_id=?, occurrence_date='2026-10-10' WHERE id=?",
						sb, other));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE tb_order_service SET occurrence_date='2026-10-10' WHERE id=?", other));
	}

}
