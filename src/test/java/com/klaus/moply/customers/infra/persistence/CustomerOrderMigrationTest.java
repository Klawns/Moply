package com.klaus.moply.customers.infra.persistence;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.klaus.moply.factory.PostgresTestDatabase;

import static org.junit.jupiter.api.Assertions.*;

class CustomerOrderMigrationTest {

	private DriverManagerDataSource dataSource;

	private JdbcTemplate jdbc;

	private String schema;

	@BeforeEach
	void createIsolatedSchema() {
		schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
		var postgres = PostgresTestDatabase.POSTGRES;
		dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("CREATE SCHEMA " + schema);
		var properties = new Properties();
		properties.setProperty("currentSchema", schema);
		dataSource.setConnectionProperties(properties);
	}

	@AfterEach
	void dropIsolatedSchema() {
		jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
	}

	@Test
	void shouldMigrateEmptyDatabaseThroughV6AndValidateHistory() {
		var flyway = flyway("6");
		assertEquals(6, flyway.migrate().migrationsExecuted);
		assertEquals(List.of("1", "2", "3", "4", "5", "6"),
				Arrays.stream(flyway.info().applied()).map(migration -> migration.getVersion().getVersion()).toList());
		flyway.validate();
		assertEquals(0, flyway.migrate().migrationsExecuted);
		assertEquals(0, count("tb_order_service"));
		assertEquals(0, count("tb_customer"));
		assertEquals(0, count("tb_customer_location"));
		assertFalse(hasColumn("customer"));
		assertTrue(hasColumn("customer_id"));
		assertEquals(List.of("idx_order_service_customer_id", "idx_service_date", "tb_prestacao_servico_pkey"), jdbc
			.queryForList(
					"SELECT indexname FROM pg_indexes WHERE schemaname = ? AND tablename = 'tb_order_service' ORDER BY indexname",
					String.class, schema));
	}

	@Test
	void shouldCreateDistinctCustomersForHomonymsWithoutMergingExistingCustomer() {
		flyway("4").migrate();
		var existingCustomer = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO tb_customer (id, name, phone, email, notes) VALUES (?, 'Maria', '123', 'm@a', 'Existente')",
				existingCustomer);
		jdbc.update("INSERT INTO tb_customer_location (id, customer_id, name) VALUES (?, ?, 'Casa')", UUID.randomUUID(),
				existingCustomer);
		var customersBefore = jdbc.queryForList("SELECT * FROM tb_customer");
		var locationsBefore = jdbc.queryForList("SELECT * FROM tb_customer_location");
		var first = insertLegacyOrder("  Maria  ");
		var second = insertLegacyOrder("Maria");
		var ordersBefore = jdbc.queryForList(
				"SELECT id, contracted_hours, hourly_rate, employee_count, service_date FROM tb_order_service ORDER BY id");

		assertEquals(2, flyway("6").migrate().migrationsExecuted);

		var firstCustomer = customerId(first);
		var secondCustomer = customerId(second);
		assertNotEquals(firstCustomer, secondCustomer);
		assertNotEquals(existingCustomer, firstCustomer);
		assertNotEquals(existingCustomer, secondCustomer);
		assertEquals(3, count("tb_customer"));
		assertEquals(List.of("Maria", "Maria"),
				jdbc.queryForList("SELECT name FROM tb_customer WHERE id <> ?", String.class, existingCustomer));
		assertEquals(2, jdbc.queryForObject(
				"SELECT count(*) FROM tb_customer WHERE id <> ? AND phone IS NULL AND email IS NULL AND notes IS NULL",
				Integer.class, existingCustomer));
		assertEquals(customersBefore, jdbc.queryForList("SELECT * FROM tb_customer WHERE id = ?", existingCustomer));
		assertEquals(locationsBefore, jdbc.queryForList("SELECT * FROM tb_customer_location"));
		assertEquals(ordersBefore, jdbc.queryForList(
				"SELECT id, contracted_hours, hourly_rate, employee_count, service_date FROM tb_order_service ORDER BY id"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   ", "\t", "\r\n", " \t\n " })
	void shouldRollbackV5ForInvalidNamesAndAllowRetryAfterCorrection(String invalidName) {
		flyway("4").migrate();
		insertLegacyOrder("Cliente válido");
		var invalidOrder = insertLegacyOrder(invalidName);
		var ordersBefore = jdbc.queryForList("SELECT * FROM tb_order_service ORDER BY id");
		var historyBefore = jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");

		var failure = assertThrows(FlywayException.class, () -> flyway("6").migrate());

		assertTrue(failure.getMessage().contains("invalid historical customer names"), failure.getMessage());
		assertEquals(ordersBefore, jdbc.queryForList("SELECT * FROM tb_order_service ORDER BY id"));
		assertEquals(historyBefore, jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank"));
		assertEquals(0, count("tb_customer"));
		assertFalse(hasColumn("customer_id"));
		assertEquals("character varying", jdbc.queryForObject(
				"SELECT data_type FROM information_schema.columns WHERE table_schema = ? AND table_name = 'tb_order_service' AND column_name = 'customer'",
				String.class, schema));
		jdbc.update("UPDATE tb_order_service SET customer = 'Corrigido' WHERE id = ?", invalidOrder);
		assertEquals(2, flyway("6").migrate().migrationsExecuted);
		flyway("6").validate();
		assertEquals(2, count("tb_customer"));
		assertNotNull(customerId(invalidOrder));
	}

	@Test
	void shouldPreserveV5LinksAndConstraintsWhenV6RemovesHistoricalName() {
		flyway("4").migrate();
		var order = insertLegacyOrder("  Maria  ");
		flyway("5").migrate();
		assertEquals("  Maria  ",
				jdbc.queryForObject("SELECT customer FROM tb_order_service WHERE id = ?", String.class, order));
		var linkedCustomer = customerId(order);
		var customersBefore = jdbc.queryForList("SELECT * FROM tb_customer ORDER BY id");
		var ordersBefore = jdbc.queryForList(
				"SELECT id, customer_id, contracted_hours, hourly_rate, employee_count, service_date FROM tb_order_service ORDER BY id");

		assertEquals(1, flyway("6").migrate().migrationsExecuted);

		assertFalse(hasColumn("customer"));
		assertEquals(customersBefore, jdbc.queryForList("SELECT * FROM tb_customer ORDER BY id"));
		assertEquals(ordersBefore, jdbc.queryForList(
				"SELECT id, customer_id, contracted_hours, hourly_rate, employee_count, service_date FROM tb_order_service ORDER BY id"));
		jdbc.update("UPDATE tb_customer SET name = 'Maria Silva' WHERE id = ?", linkedCustomer);
		assertEquals("Maria Silva", jdbc.queryForObject(
				"SELECT c.name FROM tb_order_service o JOIN tb_customer c ON c.id = o.customer_id WHERE o.id = ?",
				String.class, order));
		assertEquals(linkedCustomer, customerId(order));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc
			.update("UPDATE tb_order_service SET customer_id = ? WHERE id = ?", UUID.randomUUID(), order));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE tb_order_service SET customer_id = NULL WHERE id = ?", order));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("DELETE FROM tb_customer WHERE id = ?", linkedCustomer));
		assertEquals(linkedCustomer, customerId(order));
		flyway("6").validate();
	}

	private Flyway flyway(String target) {
		return Flyway.configure()
			.dataSource(dataSource)
			.locations("classpath:db/migration")
			.defaultSchema(schema)
			.schemas(schema)
			.target(target)
			.load();
	}

	private UUID insertLegacyOrder(String customer) {
		var id = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO tb_order_service (id, customer, contracted_hours, hourly_rate, employee_count, service_date) VALUES (?, ?, 4.50, 11.50, 3, DATE '2026-09-28')",
				id, customer);
		return id;
	}

	private UUID customerId(UUID order) {
		return jdbc.queryForObject("SELECT customer_id FROM tb_order_service WHERE id = ?", UUID.class, order);
	}

	private int count(String table) {
		return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
	}

	private boolean hasColumn(String column) {
		return jdbc.queryForObject(
				"SELECT count(*) > 0 FROM information_schema.columns WHERE table_schema = ? AND table_name = 'tb_order_service' AND column_name = ?",
				Boolean.class, schema, column);
	}

}
