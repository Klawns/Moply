package com.klaus.moply.customers.infra.persistence;

import static com.klaus.moply.factory.AccountFixture.ACCOUNT;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.ports.LocationReadRepository;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.shared.application.pagination.PageQuery;
import jakarta.persistence.EntityManager;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ CustomerJpaRepositoryAdapter.class, LocationReadJpaRepositoryAdapter.class })
class LocationReadRepositoryIntegrationTest {

	@Autowired
	CustomerRepository customers;

	@Autowired
	LocationReadRepository locations;

	@Autowired
	EntityManager entities;

	@Test
	void shouldScopeSearchAndPageLocationsWithCustomerIdentity() {
		var customer = customers.save(ACCOUNT,
				Customer.create("Maria Silva", null, null, null,
						List.of(CustomerLocation.create("Casa", "Rua Aurora", null),
								CustomerLocation.create("Casa", "Rua Norte", null),
								CustomerLocation.create("100%_local", null, null))));
		customers.save(UUID.randomUUID(), Customer.create("Outra conta", null, null, null,
				List.of(CustomerLocation.create("Casa", "Rua Aurora", null))));
		entities.flush();
		entities.clear();
		var first = locations.findAll(ACCOUNT, null, new PageQuery(0, 1, null));
		var second = locations.findAll(ACCOUNT, null, new PageQuery(1, 1, null));
		assertEquals(3, first.totalElements());
		assertEquals(3, first.totalPages());
		assertNotEquals(first.content().getFirst().id(), second.content().getFirst().id());
		assertEquals(customer.getId(), second.content().getFirst().customerId());
		assertEquals("Maria Silva", second.content().getFirst().customerName());
		assertEquals(2, locations.findAll(ACCOUNT, "CASA", PageQuery.defaults()).totalElements());
		assertEquals(1, locations.findAll(ACCOUNT, "aUrOrA", PageQuery.defaults()).totalElements());
		assertEquals(3, locations.findAll(ACCOUNT, "SILVA", PageQuery.defaults()).totalElements());
		assertEquals(1, locations.findAll(ACCOUNT, "%_", PageQuery.defaults()).totalElements());
		assertEquals(0, locations.findAll(ACCOUNT, "Outra conta", PageQuery.defaults()).totalElements());
		assertEquals(0, locations.findAll(ACCOUNT, null, new PageQuery(3, 1, null)).content().size());
	}

}
