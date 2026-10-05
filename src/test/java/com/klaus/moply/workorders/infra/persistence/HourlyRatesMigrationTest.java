package com.klaus.moply.workorders.infra.persistence;

import java.math.BigDecimal;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import com.klaus.moply.factory.PostgresTestDatabase;

class HourlyRatesMigrationTest {

	DriverManagerDataSource source;

	JdbcTemplate jdbc;

	String schema;

	@BeforeEach
	void setup() {
		var pg = PostgresTestDatabase.POSTGRES;
		schema = "rates_" + UUID.randomUUID().toString().replace("-", "");
		source = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
		new JdbcTemplate(source).execute("CREATE SCHEMA " + schema);
		var properties = new Properties();
		properties.setProperty("currentSchema", schema);
		source.setConnectionProperties(properties);
		jdbc = new JdbcTemplate(source);
	}

	@AfterEach
	void cleanup() {
		jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
	}

	Flyway flyway(String target) {
		return Flyway.configure().dataSource(source).schemas(schema).target(target).load();
	}

	@Test
	void shouldPreserveExistingWorksSeriesAndSettlementsAndValidateOptionalRates() {
		flyway("15").migrate();
		UUID org = UUID.randomUUID(), customer = UUID.randomUUID(), person = UUID.randomUUID(),
				work = UUID.randomUUID(), series = UUID.randomUUID();
		jdbc.update("INSERT INTO tb_organization VALUES (?,'Account','GBP','UTC','SCHEDULED')", org);
		jdbc.update("INSERT INTO tb_customer(id,organization_id,name) VALUES (?,?,'Client')", customer, org);
		jdbc.update("INSERT INTO tb_collaborator(id,organization_id,name) VALUES (?,?,'Worker')", person, org);
		jdbc.update(
				"INSERT INTO tb_order_service(id,organization_id,customer_id,service_date,contracted_hours,hourly_rate,currency_code,total_amount,allocation_policy_version,status) VALUES (?,?,?,CURRENT_DATE,4,30,'GBP',120,1,'SCHEDULED')",
				work, org, customer);
		jdbc.update(
				"INSERT INTO tb_work_assignment(id,organization_id,work_order_id,collaborator_id,inclusion_position,allocated_amount) VALUES (?,?,?,?,0,120)",
				UUID.randomUUID(), org, work, person);
		jdbc.update(
				"INSERT INTO tb_recurrence_series(id,organization_id,frequency,starts_on,customer_id,contracted_hours,hourly_rate,currency_code,initial_status,family_id,first_position) VALUES (?,?,'WEEKLY',CURRENT_DATE,?,4,30,'GBP','SCHEDULED',?,0)",
				series, org, customer, series);
		jdbc.update(
				"INSERT INTO tb_recurrence_member(id,organization_id,series_id,collaborator_id,inclusion_position) VALUES (?,?,?,?,0)",
				UUID.randomUUID(), org, series, person);
		jdbc.update(
				"INSERT INTO tb_collaborator_payment(id,organization_id,work_order_id,collaborator_id,idempotency_key,amount,currency_code,paid_on,recorded_at,recorded_by,status) VALUES (?,?,?,?,'partial',20,'GBP',CURRENT_DATE,CURRENT_TIMESTAMP,?,'RECORDED')",
				UUID.randomUUID(), org, work, person, UUID.randomUUID());
		var before = jdbc.queryForList("SELECT * FROM tb_order_service");
		var payments = jdbc.queryForList("SELECT * FROM tb_collaborator_payment");
		assertEquals(1, flyway("16").migrate().migrationsExecuted);
		flyway("16").validate();
		assertEquals(0, flyway("16").migrate().migrationsExecuted);
		assertEquals(before, jdbc.queryForList("SELECT * FROM tb_order_service"));
		assertEquals(payments, jdbc.queryForList("SELECT * FROM tb_collaborator_payment"));
		assertNull(jdbc.queryForObject("SELECT hourly_rate FROM tb_collaborator", BigDecimal.class));
		assertNull(jdbc.queryForObject("SELECT total_amount FROM tb_recurrence_series", BigDecimal.class));
		assertNull(jdbc.queryForObject("SELECT applied_hourly_rate FROM tb_work_assignment", BigDecimal.class));
		for (String invalid : List.of("0", "-1", "1.001", "'Infinity'::numeric", "'NaN'::numeric")) {
			assertThrows(DataIntegrityViolationException.class,
					() -> jdbc.update("UPDATE tb_collaborator SET hourly_rate=" + invalid));
			assertThrows(DataIntegrityViolationException.class,
					() -> jdbc.update("UPDATE tb_organization SET default_hourly_rate=" + invalid));
		}
		jdbc.update("UPDATE tb_collaborator SET hourly_rate=20");
		jdbc.update("UPDATE tb_organization SET default_hourly_rate=30");
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE tb_work_assignment SET base_amount=100"));
		jdbc.update(
				"UPDATE tb_work_assignment SET applied_hourly_rate=30,fixed_rate=false,base_amount=100,surplus_amount=20");
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE tb_work_assignment SET surplus_amount=21"));
	}

}
