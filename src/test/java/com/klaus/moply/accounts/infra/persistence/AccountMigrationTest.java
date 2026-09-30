package com.klaus.moply.accounts.infra.persistence;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class AccountMigrationTest {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-bookworm");

	@Test
	void shouldUpgradeV6WithoutChangingExistingOperationalDataOrCreatingAccounts() {
		var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
		var jdbc = new JdbcTemplate(source);
		Flyway.configure().dataSource(source).target("6").load().migrate();
		var customer = UUID.randomUUID();
		jdbc.update("INSERT INTO tb_customer (id, name) VALUES (?, 'Maria')", customer);
		jdbc.update("INSERT INTO tb_customer_location (id, customer_id, name) VALUES (?, ?, 'Casa')", UUID.randomUUID(),
				customer);
		jdbc.update(
				"INSERT INTO tb_order_service (id, customer_id, contracted_hours, hourly_rate, employee_count, service_date) VALUES (?, ?, 4, 11.50, 2, DATE '2026-09-28')",
				UUID.randomUUID(), customer);
		var customers = jdbc.queryForList("SELECT * FROM tb_customer");
		var locations = jdbc.queryForList("SELECT * FROM tb_customer_location");
		var orders = jdbc.queryForList("SELECT * FROM tb_order_service");
		var history = jdbc.queryForList(
				"SELECT version, checksum FROM flyway_schema_history WHERE version <= '6' ORDER BY installed_rank");

		var flyway = Flyway.configure().dataSource(source).target("7").load();
		assertEquals(1, flyway.migrate().migrationsExecuted);
		flyway.validate();
		assertEquals(0, flyway.migrate().migrationsExecuted);
		assertEquals(customers, jdbc.queryForList("SELECT * FROM tb_customer"));
		assertEquals(locations, jdbc.queryForList("SELECT * FROM tb_customer_location"));
		assertEquals(orders, jdbc.queryForList("SELECT * FROM tb_order_service"));
		assertEquals(history, jdbc.queryForList(
				"SELECT version, checksum FROM flyway_schema_history WHERE version <= '6' ORDER BY installed_rank"));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_organization", Integer.class));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_app_user", Integer.class));
	}

}
