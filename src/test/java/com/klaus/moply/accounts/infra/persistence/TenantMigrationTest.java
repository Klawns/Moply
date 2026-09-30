package com.klaus.moply.accounts.infra.persistence;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class TenantMigrationTest {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-bookworm");

	private DriverManagerDataSource source;

	private JdbcTemplate jdbc;

	private String schema;

	@BeforeEach
	void setup() {
		source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
		jdbc = new JdbcTemplate(source);
		schema = "tenant_" + UUID.randomUUID().toString().replace("-", "");
		jdbc.execute("CREATE SCHEMA " + schema);
		source = new DriverManagerDataSource(
				postgres.getJdbcUrl() + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema,
				postgres.getUsername(), postgres.getPassword());
		jdbc = new JdbcTemplate(source);
	}

	@AfterEach
	void cleanup() {
		jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
	}

	private Flyway flyway(String version) {
		return Flyway.configure().dataSource(source).schemas(schema).target(version).load();
	}

	@Test
	void shouldApplyV8FromEmptyAndRejectCrossAccountReferences() {
		assertEquals(8, flyway("8").migrate().migrationsExecuted);
		flyway("8").validate();
		assertEquals(0, flyway("8").migrate().migrationsExecuted);
		UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
		for (UUID id : new UUID[] { a, b })
			jdbc.update("INSERT INTO tb_organization VALUES (?, 'Account', 'GBP', 'UTC', 'SCHEDULED')", id);
		jdbc.update("INSERT INTO tb_customer(id,organization_id,name) VALUES (?,?,'Maria')", c, a);
		assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
				() -> jdbc.update("INSERT INTO tb_customer(id,name) VALUES (?,'Missing account')", UUID.randomUUID()));
		assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
				() -> jdbc.update(
						"INSERT INTO tb_customer_location(id,organization_id,customer_id,name) VALUES (?,?,?,'Casa')",
						UUID.randomUUID(), b, c));
		assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
				"INSERT INTO tb_order_service(id,organization_id,customer_id,contracted_hours,hourly_rate,employee_count,service_date) VALUES (?,?,?,4,10,2,CURRENT_DATE)",
				UUID.randomUUID(), b, c));
		jdbc.update("INSERT INTO tb_customer_location(id,organization_id,customer_id,name) VALUES (?,?,?,'Casa')",
				UUID.randomUUID(), a, c);
		jdbc.update(
				"INSERT INTO tb_order_service(id,organization_id,customer_id,contracted_hours,hourly_rate,employee_count,service_date) VALUES (?,?,?,4,10,2,CURRENT_DATE)",
				UUID.randomUUID(), a, c);
	}

	@Test
	void shouldRejectLegacyDataWithoutDeletingOrRewritingHistory() {
		flyway("7").migrate();
		var history = jdbc.queryForList("SELECT version,checksum FROM flyway_schema_history ORDER BY installed_rank");
		var id = UUID.randomUUID();
		jdbc.update("INSERT INTO tb_customer(id,name) VALUES (?,'Legacy')", id);
		assertThrows(org.flywaydb.core.api.FlywayException.class, () -> flyway("8").migrate());
		assertEquals("Legacy", jdbc.queryForObject("SELECT name FROM tb_customer WHERE id=?", String.class, id));
		assertEquals(history,
				jdbc.queryForList("SELECT version,checksum FROM flyway_schema_history ORDER BY installed_rank"));
		assertEquals(0, jdbc.queryForObject(
				"SELECT count(*) FROM information_schema.columns WHERE table_schema=? AND table_name='tb_customer' AND column_name='organization_id'",
				Integer.class, schema));
		jdbc.update("DELETE FROM tb_customer");
		assertEquals(1, flyway("8").migrate().migrationsExecuted);
		assertEquals(history, jdbc.queryForList(
				"SELECT version,checksum FROM flyway_schema_history WHERE version<>'8' ORDER BY installed_rank"));
	}

}
