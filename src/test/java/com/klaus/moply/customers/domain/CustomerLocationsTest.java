package com.klaus.moply.customers.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.customers.domain.exception.CustomerLocationNotFoundException;
import com.klaus.moply.orderservice.domain.exception.DomainException;

class CustomerLocationsTest {

	@Test
	void shouldAllowCustomerWithoutLocations() {
		assertTrue(Customer.create("Maria").getLocations().isEmpty());
	}

	@Test
	void shouldKeepLocationsImmutableAndPreserveThemWhenUpdatingCustomer() {
		var first = CustomerLocation.create("Casa", null, null);
		var second = CustomerLocation.create("Casa", "Rua 2", null);
		var locations = new ArrayList<>(List.of(first));
		UUID id = UUID.randomUUID();
		var original = Customer.restore(id, "Maria", null, null, null, locations);
		locations.clear();
		var customer = original.addLocation(second).update("Ana", null, null, null);
		assertEquals(id, customer.getId());
		assertEquals(List.of(first, second), customer.getLocations());
		assertEquals(List.of(first), original.getLocations());
		assertThrows(UnsupportedOperationException.class, () -> customer.getLocations().clear());
	}

	@Test
	void shouldUpdateOnlySelectedLocationAndKeepCustomerContacts() {
		var first = CustomerLocation.create("Casa", "Rua 1", null);
		var second = CustomerLocation.create("Escritório", "Rua 2", null);
		var original = Customer.restore(UUID.randomUUID(), "Maria", "123", "a@b", "cliente", List.of(first, second));
		var updated = original.updateLocation(first.getId(), "Nova casa", null, "local");
		assertEquals(first.getId(), updated.getLocations().getFirst().getId());
		assertNull(updated.getLocations().getFirst().getAddress());
		assertEquals("local", updated.getLocations().getFirst().getNotes());
		assertSame(second, updated.getLocations().get(1));
		assertEquals("cliente", updated.getNotes());
		assertEquals("123", updated.getPhone().value());
		assertEquals("a@b", updated.getEmail().value());
		assertEquals("Casa", original.findLocation(first.getId()).getName());
	}

	@Test
	void shouldRejectLocationOutsideCustomer() {
		var other = CustomerLocation.create("Casa", null, null);
		var customer = Customer.create("Maria");
		assertThrows(CustomerLocationNotFoundException.class, () -> customer.findLocation(other.getId()));
		assertThrows(CustomerLocationNotFoundException.class,
				() -> customer.updateLocation(other.getId(), "Outro", null, null));
	}

	@Test
	void shouldRejectDuplicateIdentityAndNullLocations() {
		var location = CustomerLocation.create("Casa", null, null);
		var customer = Customer.create("Maria").addLocation(location);
		assertThrows(DomainException.class, () -> customer.addLocation(location));
		assertThrows(DomainException.class, () -> customer.addLocation(null));
		assertThrows(DomainException.class, () -> Customer.create("Maria", null, null, null, null));
		assertThrows(DomainException.class,
				() -> Customer.restore(UUID.randomUUID(), "Maria", null, null, null, Arrays.asList(location, null)));
		assertThrows(DomainException.class,
				() -> Customer.restore(UUID.randomUUID(), "Maria", null, null, null, List.of(location, location)));
	}

	@Test
	void shouldRejectNullLocationIdExplicitlyWithOrWithoutLocations() {
		var empty = Customer.create("Maria");
		var populated = empty.addLocation(CustomerLocation.create("Casa", null, null));
		for (Customer customer : List.of(empty, populated)) {
			var lookupError = assertThrowsExactly(DomainException.class, () -> customer.findLocation(null));
			var updateError = assertThrowsExactly(DomainException.class,
					() -> customer.updateLocation(null, "Casa", null, null));
			assertEquals("O ID do local é obrigatório.", lookupError.getMessage());
			assertEquals(lookupError.getMessage(), updateError.getMessage());
		}
	}

	@Test
	void shouldRejectNullLocationAtAddition() {
		var customer = Customer.create("Maria");
		var error = assertThrowsExactly(DomainException.class, () -> customer.addLocation(null));
		assertEquals("O local não pode ser nulo.", error.getMessage());
		assertTrue(customer.getLocations().isEmpty());
	}

}
