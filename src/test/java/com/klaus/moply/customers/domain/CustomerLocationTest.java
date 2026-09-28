package com.klaus.moply.customers.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.orderservice.domain.exception.DomainException;

class CustomerLocationTest {

	@Test
	void shouldCreateIdentifiedLocationWithoutAddress() {
		var location = CustomerLocation.create(" Casa ", null, " ");
		assertNotNull(location.getId());
		assertEquals("Casa", location.getName());
		assertNull(location.getAddress());
		assertNull(location.getNotes());
	}

	@Test
	void shouldRestoreAndUpdatePreservingIdentity() {
		UUID id = UUID.randomUUID();
		var location = CustomerLocation.restore(id, "Casa", " Rua 1 ", " Portão azul ");
		assertEquals("Rua 1", location.getAddress());
		assertEquals("Portão azul", location.getNotes());
		var updated = location.update(" Escritório ", null, null);
		assertEquals(id, updated.getId());
		assertEquals("Escritório", updated.getName());
		assertNull(updated.getAddress());
		assertEquals("Casa", location.getName());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n" })
	void shouldRejectInvalidNameOnCreationRestoreAndUpdate(String name) {
		assertThrows(DomainException.class, () -> CustomerLocation.create(name, null, null));
		assertThrows(DomainException.class, () -> CustomerLocation.restore(UUID.randomUUID(), name, null, null));
		var location = CustomerLocation.create("Casa", null, null);
		assertThrows(DomainException.class, () -> location.update(name, null, null));
	}

	@Test
	void shouldRequireIdentityOnRestore() {
		assertThrows(DomainException.class, () -> CustomerLocation.restore(null, "Casa", null, null));
	}

}
