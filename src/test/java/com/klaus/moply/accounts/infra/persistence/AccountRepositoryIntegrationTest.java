package com.klaus.moply.accounts.infra.persistence;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.klaus.moply.accounts.application.exception.AccountConflictException;
import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.AccountRegistration;
import com.klaus.moply.accounts.application.ports.AppUserRepository;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.accounts.domain.vo.Organization;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = { "spring.jpa.open-in-view=false", "spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate" })
@ActiveProfiles("test")
@Testcontainers
class AccountRepositoryIntegrationTest {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-bookworm");

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry properties) {
		properties.add("spring.datasource.url", postgres::getJdbcUrl);
		properties.add("spring.datasource.username", postgres::getUsername);
		properties.add("spring.datasource.password", postgres::getPassword);
		properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		properties.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
	}

	@Autowired
	AccountRegistration registration;

	@Autowired
	OrganizationRepository organizations;

	@Autowired
	AppUserRepository users;

	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM tb_app_user");
		jdbc.update("DELETE FROM tb_organization");
	}

	@Test
	void shouldRegisterAndReadAccountAndManagerOutsideTransaction() {
		var organization = Organization.create("Empresa", "UTC");
		var manager = manager(organization, " OWNER@EXAMPLE.COM ");
		registration.register(organization, manager);

		assertEquals(organization, organizations.findById(organization.id()).orElseThrow());
		var stored = users.findById(manager.getId()).orElseThrow();
		assertEquals(organization.id(), stored.getOrganizationId());
		assertEquals(manager.getEmail(), stored.getEmail());
		assertEquals(manager.getPasswordHash(), stored.getPasswordHash());
		assertEquals(manager.getId(), users.findByEmail(new LoginEmail("owner@example.com")).orElseThrow().getId());
		assertTrue(users.findById(UUID.randomUUID()).isEmpty());
		assertTrue(users.findByEmail(new LoginEmail("missing@example.com")).isEmpty());
		assertTrue(organizations.findById(UUID.randomUUID()).isEmpty());
	}

	@Test
	void shouldPersistPreferencesAndRejectUpdateOfMissingAccount() {
		var organization = Organization.create("Empresa", "UTC");
		registration.register(organization, manager(organization, "owner@example.com"));
		var changed = organization.withPreferences("America/Sao_Paulo", DefaultWorkStatus.COMPLETED);

		assertEquals(changed, organizations.update(changed));
		assertEquals(changed, organizations.findById(organization.id()).orElseThrow());
		assertThrows(AccountNotFoundException.class, () -> organizations.update(Organization.create("Missing", "UTC")));
	}

	@Test
	void shouldRollbackOrganizationOnDuplicateNormalizedEmail() {
		var first = Organization.create("First", "UTC");
		registration.register(first, manager(first, "owner@example.com"));
		var second = Organization.create("Second", "UTC");

		assertThrows(AccountConflictException.class,
				() -> registration.register(second, manager(second, " OWNER@EXAMPLE.COM ")));
		assertTrue(organizations.findById(second.id()).isEmpty());
		assertEquals(first, organizations.findById(first.id()).orElseThrow());
		assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM tb_organization", Integer.class));
		assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM tb_app_user", Integer.class));
	}

	@Test
	void shouldRejectManagerFromDifferentAccountWithoutPersisting() {
		var organization = Organization.create("Empresa", "UTC");
		var other = Organization.create("Other", "UTC");

		var failure = assertThrows(InvalidDataAccessApiUsageException.class,
				() -> registration.register(organization, manager(other, "owner@example.com")));
		assertInstanceOf(IllegalArgumentException.class, failure.getCause());
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_organization", Integer.class));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_app_user", Integer.class));
	}

	private AppUser manager(Organization organization, String email) {
		return new AppUser(UUID.randomUUID(), organization.id(), new LoginEmail(email), "encoded-fixture");
	}

}
