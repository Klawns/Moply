package com.klaus.moply.customers.infra.persistence;

import static com.klaus.moply.factory.AccountFixture.*;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.shared.application.pagination.PageQuery;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import(CustomerJpaRepositoryAdapter.class)
class CustomerRepositoryIntegrationTest {

	@Autowired
	CustomerRepository repo;

	@Autowired
	EntityManager entityManager;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Test
	void shouldPersistFullAggregateAndPreserveIdentitiesOnUpdate() {
		var home = CustomerLocation.create("Casa", null, null);
		var office = CustomerLocation.create("Escritório", "Rua A", "Portaria");
		var saved = repo.save(ACCOUNT, Customer.create("Maria", "123", "a@b", "Notas", List.of(home, office)));
		assertNotNull(saved.getId());
		entityManager.clear();
		var loaded = repo.findById(ACCOUNT, saved.getId()).orElseThrow();
		assertEquals("123", loaded.getPhone().value());
		assertEquals("a@b", loaded.getEmail().value());
		assertEquals("Notas", loaded.getNotes());
		assertNull(loaded.findLocation(home.getId()).getAddress());
		var updated = repo.save(ACCOUNT, loaded.update("Maria Silva", "456", null, null)
			.updateLocation(home.getId(), "Casa nova", "Rua B", null));
		entityManager.clear();
		var result = repo.findById(ACCOUNT, saved.getId()).orElseThrow();
		assertEquals(saved.getId(), updated.getId());
		assertEquals("Maria Silva", result.getName().value());
		assertEquals(2, result.getLocations().size());
		assertEquals("Casa nova", result.findLocation(home.getId()).getName());
		assertEquals("Rua A", result.findLocation(office.getId()).getAddress());
		assertEquals(1, repo.findAll(ACCOUNT, new com.klaus.moply.shared.application.pagination.PageQuery(0, 20, null))
			.totalElements());
		var locationPage = repo.findLocations(ACCOUNT, saved.getId(), new PageQuery(1, 1, null));
		assertEquals(2, locationPage.totalElements());
		assertEquals(2, locationPage.totalPages());
		assertEquals("Escritório", locationPage.content().getFirst().getName());
	}

	@Test
	void shouldAllowHomonymsAndSaveAdditionalLocations() {
		var first = repo.save(ACCOUNT, Customer.create("Maria"));
		var second = repo.save(ACCOUNT, Customer.create("Maria"));
		assertNotEquals(first.getId(), second.getId());
		var location = CustomerLocation.create("Casa", null, null);
		repo.save(ACCOUNT, first.addLocation(location));
		entityManager.clear();
		assertEquals(location.getId(),
				repo.findById(ACCOUNT, first.getId()).orElseThrow().getLocations().getFirst().getId());
		assertTrue(repo.findById(ACCOUNT, second.getId()).orElseThrow().getLocations().isEmpty());
		assertEquals(2, repo.findAll(ACCOUNT, new com.klaus.moply.shared.application.pagination.PageQuery(0, 20, null))
			.totalElements());
		var firstPage = repo.findAll(ACCOUNT,
				new com.klaus.moply.shared.application.pagination.PageQuery(0, 1,
						new com.klaus.moply.shared.application.pagination.SortQuery("name",
								com.klaus.moply.shared.application.pagination.SortQuery.Direction.ASC)));
		var secondPage = repo.findAll(ACCOUNT,
				new com.klaus.moply.shared.application.pagination.PageQuery(1, 1,
						new com.klaus.moply.shared.application.pagination.SortQuery("name",
								com.klaus.moply.shared.application.pagination.SortQuery.Direction.ASC)));
		assertEquals(2, firstPage.totalElements());
		assertEquals(2, firstPage.totalPages());
		assertNotEquals(firstPage.content().getFirst().getId(), secondPage.content().getFirst().getId());
		assertTrue(java.util.Set.of(first.getId(), second.getId())
			.containsAll(
					java.util.Set.of(firstPage.content().getFirst().getId(), secondPage.content().getFirst().getId())));
		assertEquals(1, secondPage.page());
	}

	@Test
	void shouldRollbackCustomerChangesWhenLocationPersistenceFails() {
		TestTransaction.end();
		var transaction = new TransactionTemplate(transactionManager);
		UUID firstId = null;
		UUID secondId = null;
		try {
			var location = CustomerLocation.create("Casa", null, null);
			var first = repo.save(ACCOUNT, Customer.create("Primeiro").addLocation(location));
			firstId = first.getId();
			var second = repo.save(ACCOUNT, Customer.create("Segundo"));
			secondId = second.getId();
			assertThrows(RuntimeException.class, () -> transaction.execute(
					status -> repo.save(ACCOUNT, second.update("Alterado", null, null, null).addLocation(location))));
			assertEquals("Segundo", repo.findById(ACCOUNT, second.getId()).orElseThrow().getName().value());
			assertTrue(repo.findById(ACCOUNT, second.getId()).orElseThrow().getLocations().isEmpty());
			assertEquals(location.getId(),
					repo.findById(ACCOUNT, first.getId()).orElseThrow().getLocations().getFirst().getId());
		}
		finally {
			UUID cleanupFirst = firstId;
			UUID cleanupSecond = secondId;
			transaction.executeWithoutResult(status -> {
				if (cleanupFirst != null)
					entityManager.remove(entityManager.find(CustomerEntity.class, cleanupFirst));
				if (cleanupSecond != null)
					entityManager.remove(entityManager.find(CustomerEntity.class, cleanupSecond));
			});
		}
	}

}
