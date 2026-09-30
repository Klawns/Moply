package com.klaus.moply.factory;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Shared datasource configuration for Spring integration tests that use PostgreSQL. */
public abstract class PostgresSpringIntegrationTest {

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry properties) {
		var postgres = PostgresTestDatabase.POSTGRES;
		properties.add("spring.datasource.url", postgres::getJdbcUrl);
		properties.add("spring.datasource.username", postgres::getUsername);
		properties.add("spring.datasource.password", postgres::getPassword);
		properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		properties.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
	}

}
