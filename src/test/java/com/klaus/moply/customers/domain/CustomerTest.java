package com.klaus.moply.customers.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.orderservice.domain.exception.DomainException;

class CustomerTest {

	@Test
	void shouldCreateWithOnlyName() {
		Customer customer = Customer.create("  Maria Silva  ");
		assertNull(customer.getId());
		assertEquals("Maria Silva", customer.getName().value());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "\t\n" })
	void shouldRejectMissingName(String name) {
		assertThrows(DomainException.class, () -> Customer.create(name));
		assertThrows(DomainException.class, () -> Customer.restore(UUID.randomUUID(), name));
	}

	@Test
	void shouldRestoreIdentityAndName() {
		UUID id = UUID.randomUUID();
		Customer customer = Customer.restore(id, "  Maria  ");
		assertEquals(id, customer.getId());
		assertEquals("Maria", customer.getName().value());
	}

	@Test
	void shouldRejectMissingIdentityOnRestore() {
		assertThrows(DomainException.class, () -> Customer.restore(null, "Maria"));
	}

	@Test
	void shouldAllowSameNameWithDistinctIdentities() {
		Customer first = Customer.restore(UUID.randomUUID(), "Maria");
		Customer second = Customer.restore(UUID.randomUUID(), "Maria");
		assertEquals(first.getName().value(), second.getName().value());
		assertNotEquals(first.getId(), second.getId());
	}

	@Test
	void shouldKeepOptionalFieldsAbsentWhenCreatingWithOnlyName() {
		Customer customer = Customer.create("Maria");
		assertNull(customer.getPhone());
		assertNull(customer.getEmail());
		assertNull(customer.getNotes());
	}

	@Test
	void shouldNormalizeNameAndOptionalFields() {
		Customer customer = Customer.create("  Maria  ", "  +55 (11) 99999-1234  ", "  Maria+work@Example.com  ",
				"  Linha 1\nLinha 2  ");
		assertEquals("Maria", customer.getName().value());
		assertEquals("+55 (11) 99999-1234", customer.getPhone().value());
		assertEquals("Maria+work@Example.com", customer.getEmail().value());
		assertEquals("Linha 1\nLinha 2", customer.getNotes());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "\t\n", "\u2003" })
	void shouldTreatBlankOptionalFieldsAsAbsent(String value) {
		Customer created = Customer.create("Maria", value, value, value);
		Customer restored = Customer.restore(UUID.randomUUID(), "Maria", value, value, value);
		for (Customer customer : new Customer[] { created, restored }) {
			assertNull(customer.getPhone());
			assertNull(customer.getEmail());
			assertNull(customer.getNotes());
		}
	}

	@Test
	void shouldRestoreAllFields() {
		UUID id = UUID.randomUUID();
		Customer customer = Customer.restore(id, "Maria", "  telefone livre  ", "  Maria@example.com  ",
				"  Observação  ");
		assertEquals(id, customer.getId());
		assertEquals("Maria", customer.getName().value());
		assertEquals("telefone livre", customer.getPhone().value());
		assertEquals("Maria@example.com", customer.getEmail().value());
		assertEquals("Observação", customer.getNotes());
	}

	@ParameterizedTest
	@ValueSource(strings = { "Maria@example.com", "a@b", "maria+trabalho@example.com" })
	void shouldAcceptBasicEmailFormat(String email) {
		assertEquals(email, Customer.create("Maria", null, email, null).getEmail().value());
		assertEquals(email, Customer.restore(UUID.randomUUID(), "Maria", null, email, null).getEmail().value());
	}

	@ParameterizedTest
	@ValueSource(strings = { "sem-arroba", "@dominio", "usuario@", "a@@b", "a b@c", "a@b c", "a\tb@c", "a@b\nc",
			"a\u2003b@c" })
	void shouldRejectInvalidEmail(String email) {
		assertThrows(DomainException.class, () -> Customer.create("Maria", null, email, null));
		assertThrows(DomainException.class, () -> Customer.restore(UUID.randomUUID(), "Maria", null, email, null));
	}

	@Test
	void shouldNormalizeUpdatedValuesWithoutChangingOriginal() {
		UUID id = UUID.randomUUID();
		Customer original = Customer.restore(id, " Maria ", "123", "a@b", "nota");
		Customer updated = original.update("  João  Silva  ", " telefone livre ", " João@Example.com ", " outra nota ");
		assertEquals(id, updated.getId());
		assertEquals("João  Silva", updated.getName().value());
		assertEquals("telefone livre", updated.getPhone().value());
		assertEquals("João@Example.com", updated.getEmail().value());
		assertEquals("outra nota", updated.getNotes());
		assertEquals("Maria", original.getName().value());
		assertEquals("a@b", original.getEmail().value());
	}

	@Test
	void shouldRejectInvalidNameAndEmailOnUpdate() {
		Customer customer = Customer.create("Maria");
		assertThrows(DomainException.class, () -> customer.update(" ", null, null, null));
		assertThrows(DomainException.class, () -> customer.update("Maria", null, "inválido", null));
	}

	@Test
	void shouldClearOptionalValuesOnUpdate() {
		Customer customer = Customer.create("Maria", "123", "a@b", "nota");
		Customer updated = customer.update("Maria", " ", "\t", null);
		assertNull(updated.getPhone());
		assertNull(updated.getEmail());
		assertNull(updated.getNotes());
	}

}
