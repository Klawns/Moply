package com.klaus.moply.collaborators.infra.persistence;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class CollaboratorMigrationTest {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-bookworm");

	private DriverManagerDataSource source;

	private JdbcTemplate jdbc;

	private String schema;

	@BeforeEach
	void setup() {
		schema = "collaborators_" + UUID.randomUUID().toString().replace("-", "");
		jdbc = new JdbcTemplate(
				new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
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
	void shouldApplyEntireChainAndEnforceCollaboratorConstraintsWithoutCredentials() {
		assertEquals(9, flyway("9").migrate().migrationsExecuted);
		flyway("9").validate();
		assertEquals(0, flyway("9").migrate().migrationsExecuted);
		var a = organization();
		var b = organization();
		var id = UUID.randomUUID();
		jdbc.update("INSERT INTO tb_collaborator(id,organization_id,name) VALUES (?,?,'Maria')", id, a);
		jdbc.update("INSERT INTO tb_collaborator(id,organization_id,name) VALUES (?,?,'Maria')", UUID.randomUUID(), a);
		jdbc.update("INSERT INTO tb_collaborator(id,organization_id,name) VALUES (?,?,'Maria')", UUID.randomUUID(), b);
		assertTrue(jdbc.queryForObject("SELECT active FROM tb_collaborator WHERE id=?", Boolean.class, id));
		assertEquals(0L, jdbc.queryForObject("SELECT version FROM tb_collaborator WHERE id=?", Long.class, id));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("INSERT INTO tb_collaborator(id,name) VALUES (?,'Maria')", UUID.randomUUID()));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("INSERT INTO tb_collaborator(id,organization_id,name) VALUES (?,?,'Maria')",
						UUID.randomUUID(), UUID.randomUUID()));
		for (String name : new String[] { null, "", "   " })
			assertThrows(DataIntegrityViolationException.class,
					() -> jdbc.update("INSERT INTO tb_collaborator(id,organization_id,name) VALUES (?,?,?)",
							UUID.randomUUID(), a, name));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update(
						"INSERT INTO tb_collaborator(id,organization_id,name,active) VALUES (?,?,'Maria',NULL)",
						UUID.randomUUID(), a));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("DELETE FROM tb_organization WHERE id=?", a));
		jdbc.update("UPDATE tb_collaborator SET active=false WHERE id=?", id);
		assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM tb_collaborator", Integer.class));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_app_user", Integer.class));
		var columns = jdbc.queryForList(
				"SELECT column_name FROM information_schema.columns WHERE table_schema=? AND table_name='tb_collaborator'",
				String.class, schema);
		assertEquals(java.util.Set.of("id", "organization_id", "name", "phone", "active", "version"),
				new java.util.HashSet<>(columns));
		// The composite candidate key is usable by future participant tables.
		jdbc.execute(
				"CREATE TABLE reference_probe (organization_id UUID, collaborator_id UUID, FOREIGN KEY (organization_id, collaborator_id) REFERENCES tb_collaborator(organization_id,id))");
		jdbc.update("INSERT INTO reference_probe VALUES (?,?)", a, id);
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("INSERT INTO reference_probe VALUES (?,?)", b, id));
	}

	@Test
	void shouldUpgradeV8PreservingDataAndChecksums() {
		flyway("8").migrate();
		var account = organization();
		var customer = UUID.randomUUID();
		jdbc.update("INSERT INTO tb_customer(id,organization_id,name) VALUES (?,?,'Maria')", customer, account);
		jdbc.update("INSERT INTO tb_customer_location(id,organization_id,customer_id,name) VALUES (?,?,?,'Casa')",
				UUID.randomUUID(), account, customer);
		jdbc.update(
				"INSERT INTO tb_order_service(id,organization_id,customer_id,contracted_hours,hourly_rate,employee_count,service_date) VALUES (?,?,?,4,10,2,CURRENT_DATE)",
				UUID.randomUUID(), account, customer);
		var customers = jdbc.queryForList("SELECT * FROM tb_customer");
		var locations = jdbc.queryForList("SELECT * FROM tb_customer_location");
		var orders = jdbc.queryForList("SELECT * FROM tb_order_service");
		var accounts = jdbc.queryForList("SELECT * FROM tb_organization");
		var history = jdbc.queryForList("SELECT version,checksum FROM flyway_schema_history ORDER BY installed_rank");
		assertEquals(1, flyway("9").migrate().migrationsExecuted);
		flyway("9").validate();
		assertEquals(0, flyway("9").migrate().migrationsExecuted);
		assertEquals(customers, jdbc.queryForList("SELECT * FROM tb_customer"));
		assertEquals(locations, jdbc.queryForList("SELECT * FROM tb_customer_location"));
		assertEquals(orders, jdbc.queryForList("SELECT * FROM tb_order_service"));
		assertEquals(accounts, jdbc.queryForList("SELECT * FROM tb_organization"));
		assertEquals(history, jdbc.queryForList(
				"SELECT version,checksum FROM flyway_schema_history WHERE version<>'9' ORDER BY installed_rank"));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_collaborator", Integer.class));
	}

	private UUID organization() {
		var id = UUID.randomUUID();
		jdbc.update("INSERT INTO tb_organization VALUES (?, 'Account', 'GBP', 'UTC', 'SCHEDULED')", id);
		return id;
	}

}
