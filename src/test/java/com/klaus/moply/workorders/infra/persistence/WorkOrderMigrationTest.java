package com.klaus.moply.workorders.infra.persistence;

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

class WorkOrderMigrationTest {

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

	@Test
	void shouldApplyEntireChainAndEnforceCompositeReferencesAndExactDecimals() {
		assertEquals(11, flyway("11").migrate().migrationsExecuted);
		flyway("11").validate();
		assertEquals(0, flyway("11").migrate().migrationsExecuted);
		assertEquals(0, jdbc.queryForObject(
				"SELECT count(*) FROM information_schema.columns WHERE table_schema=? AND column_name='employee_count'",
				Integer.class, schema));
		UUID a = account(), b = account(), ca = customer(a), cb = customer(b), other = customer(a), pa = person(a),
				pb = person(b), p2 = person(a);
		UUID location = UUID.randomUUID();
		jdbc.update("INSERT INTO tb_customer_location(id,organization_id,customer_id,name) VALUES (?,?,?,'Casa')",
				location, a, ca);
		UUID work = work(a, ca, location);
		assertThrows(DataIntegrityViolationException.class, () -> work(a, cb, null));
		assertThrows(DataIntegrityViolationException.class, () -> work(a, other, location));
		assertThrows(DataIntegrityViolationException.class, () -> work(b, cb, location));
		assignment(a, work, pa, 0, "10.00");
		assignment(a, work, p2, 1, "0.00");
		assertThrows(DataIntegrityViolationException.class, () -> assignment(a, work, pa, 2, "0"));
		assertThrows(DataIntegrityViolationException.class, () -> assignment(a, work, person(a), 0, "0"));
		assertThrows(DataIntegrityViolationException.class, () -> assignment(b, work, pb, 2, "0"));
		assertThrows(DataIntegrityViolationException.class, () -> assignment(a, work, pb, 2, "0"));
		assertThrows(DataIntegrityViolationException.class, () -> assignment(a, work, person(a), -1, "0"));
		for (String invalid : List.of("-0.01", "0.001"))
			assertThrows(DataIntegrityViolationException.class, () -> assignment(a, work, person(a), 2, invalid));
		for (String column : List.of("contracted_hours", "hourly_rate", "total_amount")) {
			for (String invalid : List.of("0", "-1", "0.001", "NaN", "Infinity", "-Infinity"))
				assertThrows(DataIntegrityViolationException.class, () -> jdbc
					.update("UPDATE tb_order_service SET " + column + "=CAST(? AS numeric) WHERE id=?", invalid, work));
			var huge = new BigDecimal("1234567890123456789012345678901234567890.12");
			jdbc.update("UPDATE tb_order_service SET " + column + "=? WHERE id=?", huge, work);
			assertEquals(huge, jdbc.queryForObject("SELECT " + column + " FROM tb_order_service WHERE id=?",
					BigDecimal.class, work));
		}
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE tb_order_service SET currency_code='USD' WHERE id=?", work));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE tb_order_service SET status='PAID' WHERE id=?", work));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("DELETE FROM tb_collaborator WHERE id=?", pa));
	}

	@Test
	void shouldRejectLegacyBeforeExpansionAndPreserveAllDataAndMigrationHistory() {
		flyway("9").migrate();
		UUID a = account(), c = customer(a);
		jdbc.update(
				"INSERT INTO tb_order_service(id,organization_id,customer_id,service_date,contracted_hours,hourly_rate,employee_count) VALUES (?,?,?,CURRENT_DATE,4,10,2)",
				UUID.randomUUID(), a, c);
		var orders = jdbc.queryForList("SELECT * FROM tb_order_service");
		var customers = jdbc.queryForList("SELECT * FROM tb_customer");
		var history = jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
		var error = assertThrows(FlywayException.class, () -> flyway("11").migrate());
		assertTrue(error.getMessage().contains("legacy work orders have no participant identities"));
		assertEquals(orders, jdbc.queryForList("SELECT * FROM tb_order_service"));
		assertEquals(customers, jdbc.queryForList("SELECT * FROM tb_customer"));
		assertEquals(history, jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank"));
		assertEquals(0, jdbc.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_schema=? AND table_name='tb_work_assignment'",
				Integer.class, schema));
	}

	@Test
	void shouldValidateAssignmentsBeforeRetiringLegacyCount() {
		flyway("10").migrate();
		UUID a = account(), c = customer(a), p = person(a), w = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO tb_order_service(id,organization_id,customer_id,service_date,contracted_hours,hourly_rate,employee_count,currency_code,total_amount,allocation_policy_version,status) VALUES (?,?,?,CURRENT_DATE,1,10,1,'GBP',10,1,'SCHEDULED')",
				w, a, c);
		assertThrows(FlywayException.class, () -> flyway("11").migrate());
		assignment(a, w, p, 1, "9.00");
		assertThrows(FlywayException.class, () -> flyway("11").migrate());
		jdbc.update("UPDATE tb_work_assignment SET inclusion_position=0,allocated_amount=10 WHERE work_order_id=?", w);
		assertEquals(1, flyway("11").migrate().migrationsExecuted);
		assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM tb_work_assignment", Integer.class));
	}

}
