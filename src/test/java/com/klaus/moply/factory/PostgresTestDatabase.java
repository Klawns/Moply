package com.klaus.moply.factory;

import org.testcontainers.postgresql.PostgreSQLContainer;

/** A single PostgreSQL instance shared by all integration tests in this JVM. */
public final class PostgresTestDatabase {

	public static final PostgreSQLContainer POSTGRES = start();

	private PostgresTestDatabase() {
	}

	private static PostgreSQLContainer start() {
		var postgres = new PostgreSQLContainer("postgres:17.6-bookworm");
		postgres.start();
		return postgres;
	}

}
