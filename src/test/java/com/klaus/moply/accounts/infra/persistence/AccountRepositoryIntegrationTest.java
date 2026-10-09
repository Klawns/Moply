package com.klaus.moply.accounts.infra.persistence;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.util.Optional;
import java.util.function.UnaryOperator;
import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.accounts.application.usecase.dto.UpdateAccountPreferencesInput;
import com.klaus.moply.accounts.application.usecase.exception.AccountConflictException;
import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.AccountRegistration;
import com.klaus.moply.accounts.application.ports.AppUserRepository;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.application.usecase.UpdateAccountPreferences;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.accounts.infra.persistence.adapters.AccountRegistrationJpaAdapter;
import com.klaus.moply.accounts.infra.persistence.adapters.AppUserJpaRepositoryAdapter;
import com.klaus.moply.accounts.infra.persistence.adapters.OrganizationJpaRepositoryAdapter;
import com.klaus.moply.factory.PostgresSpringIntegrationTest;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = { "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ AccountRegistrationJpaAdapter.class, AppUserJpaRepositoryAdapter.class,
		OrganizationJpaRepositoryAdapter.class })
@ActiveProfiles("test")
class AccountRepositoryIntegrationTest extends PostgresSpringIntegrationTest {

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

		assertEquals(changed, organizations.updatePreferences(organization.id(), current -> changed));
		assertEquals(changed, organizations.findById(organization.id()).orElseThrow());
		assertThrows(AccountNotFoundException.class,
				() -> organizations.updatePreferences(UUID.randomUUID(), UnaryOperator.identity()));
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

		assertThrows(IllegalArgumentException.class,
				() -> registration.register(organization, manager(other, "owner@example.com")));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_organization", Integer.class));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_app_user", Integer.class));
	}

	@Test
	void shouldPreserveConcurrentRateChangeWhenRequestOmitsRate() {
		var organization = Organization.create("Empresa", "UTC")
			.withPreferences("UTC", DefaultWorkStatus.SCHEDULED, new BigDecimal("10"));
		registration.register(organization, manager(organization, "owner@example.com"));
		var interleaved = new OrganizationRepository() {
			@Override
			public Optional<Organization> findById(UUID id) {
				return organizations.findById(id);
			}

			@Override
			public Organization updatePreferences(UUID id, UnaryOperator<Organization> change) {
				organizations.updatePreferences(id,
						current -> current.withPreferences("UTC", DefaultWorkStatus.SCHEDULED, new BigDecimal("20")));
				return organizations.updatePreferences(id, change);
			}
		};
		new UpdateAccountPreferences(interleaved).execute(new Context(organization.id()),
				new UpdateAccountPreferencesInput("Europe/London", DefaultWorkStatus.COMPLETED));
		var stored = organizations.findById(organization.id()).orElseThrow();
		assertEquals(new BigDecimal("20.00"), stored.defaultHourlyRate());
		assertEquals("Europe/London", stored.timezone());
		assertEquals(DefaultWorkStatus.COMPLETED, stored.defaultWorkStatus());
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldSerializeConcurrentPreferencesAndDistinguishOmittedRateFromExplicitNull(boolean rateProvided)
			throws Exception {
		var organization = Organization.create("Empresa", "UTC")
			.withPreferences("UTC", DefaultWorkStatus.SCHEDULED, new BigDecimal("10"));
		registration.register(organization, manager(organization, "owner@example.com"));
		var locked = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var rateChange = executor.submit(() -> organizations.updatePreferences(organization.id(), current -> {
				locked.countDown();
				await(release);
				return current.withPreferences("UTC", DefaultWorkStatus.SCHEDULED, new BigDecimal("20"));
			}));
			try {
				assertTrue(locked.await(5, TimeUnit.SECONDS), "First transaction must acquire the organization lock");
				var omittedOrNull = executor.submit(() -> new UpdateAccountPreferences(organizations)
					.execute(new Context(organization.id()), new UpdateAccountPreferencesInput("Europe/London",
							DefaultWorkStatus.COMPLETED, null, rateProvided)));
				assertOrganizationUpdateWaitingForLock();
				assertFalse(omittedOrNull.isDone());
				release.countDown();
				rateChange.get(5, TimeUnit.SECONDS);
				omittedOrNull.get(5, TimeUnit.SECONDS);
			}
			finally {
				release.countDown();
			}
		}
		var stored = organizations.findById(organization.id()).orElseThrow();
		assertEquals(rateProvided ? null : new BigDecimal("20.00"), stored.defaultHourlyRate());
		assertEquals("Europe/London", stored.timezone());
		assertEquals(DefaultWorkStatus.COMPLETED, stored.defaultWorkStatus());
	}

	@Test
	void shouldRollbackInvalidPreferencesAndKeepOtherOrganizationUntouched() {
		var organization = Organization.create("Empresa", "UTC")
			.withPreferences("UTC", DefaultWorkStatus.SCHEDULED, new BigDecimal("10"));
		var other = Organization.create("Other", "UTC");
		registration.register(organization, manager(organization, "owner@example.com"));
		registration.register(other, manager(other, "other@example.com"));
		var update = new UpdateAccountPreferences(organizations);
		assertThrows(DomainException.class, () -> update.execute(new Context(organization.id()),
				new UpdateAccountPreferencesInput("invalid", DefaultWorkStatus.COMPLETED, null, true)));
		assertEquals(organization, organizations.findById(organization.id()).orElseThrow());
		update.execute(new Context(organization.id()),
				new UpdateAccountPreferencesInput("UTC", DefaultWorkStatus.COMPLETED, new BigDecimal("30"), true));
		assertEquals(new BigDecimal("30.00"),
				organizations.findById(organization.id()).orElseThrow().defaultHourlyRate());
		assertEquals(other, organizations.findById(other.id()).orElseThrow());
	}

	private void assertOrganizationUpdateWaitingForLock() throws InterruptedException {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			var waiting = jdbc.queryForObject("""
					SELECT count(*) FROM pg_stat_activity
					WHERE datname = current_database() AND wait_event_type = 'Lock'
					AND query LIKE '%tb_organization%'
					""", Integer.class);
			if (waiting > 0) {
				return;
			}
			Thread.sleep(20);
		}
		fail("Concurrent preferences update must wait for the PostgreSQL organization lock");
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) {
				throw new AssertionError("Timed out waiting to release the first transaction");
			}
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		}
	}

	private AppUser manager(Organization organization, String email) {
		return new AppUser(UUID.randomUUID(), organization.id(), new LoginEmail(email), "encoded-fixture");
	}

}
