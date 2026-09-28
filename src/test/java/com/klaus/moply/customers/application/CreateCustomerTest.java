package com.klaus.moply.customers.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.CreateCustomer;
import com.klaus.moply.customers.application.usecase.dto.CreateCustomerInput;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.orderservice.domain.exception.DomainException;
import com.klaus.moply.factory.CustomerFactory;

class CreateCustomerTest {

	private final CustomerRepository repo = mock(CustomerRepository.class);

	private final CreateCustomer useCase = new CreateCustomer(repo);

	@Test
	void shouldSaveNameAndReturnAssignedIdentity() {
		UUID id = UUID.randomUUID();
		Customer saved = CustomerFactory.restoreCustomer(id);
		when(repo.save(any(Customer.class))).thenReturn(saved);
		assertEquals(id, useCase.execute(new CreateCustomerInput(saved.getName().value())));
		ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
		verify(repo).save(captor.capture());
		assertNull(captor.getValue().getId());
		assertEquals(saved.getName().value(), captor.getValue().getName().value());
		verifyNoMoreInteractions(repo);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "\t\n" })
	void shouldRejectInvalidNameBeforePersistence(String name) {
		assertThrows(DomainException.class, () -> useCase.execute(new CreateCustomerInput(name)));
		verifyNoInteractions(repo);
	}

	@Test
	void shouldForwardAllFieldsAndReturnSavedIdentity() {
		UUID id = UUID.randomUUID();
		when(repo.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer customer = invocation.getArgument(0);
			return Customer.restore(id, customer.getName().value(), customer.getPhone().value(),
					customer.getEmail().value(), customer.getNotes());
		});
		assertEquals(id, useCase.execute(
				new CreateCustomerInput("  Maria  ", "  +55 11 1234  ", "  Maria@example.com  ", "  Preferência  ")));
		ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
		verify(repo).save(captor.capture());
		Customer customer = captor.getValue();
		assertNull(customer.getId());
		assertEquals("Maria", customer.getName().value());
		assertEquals("+55 11 1234", customer.getPhone().value());
		assertEquals("Maria@example.com", customer.getEmail().value());
		assertEquals("Preferência", customer.getNotes());
		verifyNoMoreInteractions(repo);
	}

	@Test
	void shouldRejectInvalidEmailBeforePersistence() {
		assertThrows(DomainException.class,
				() -> useCase.execute(new CreateCustomerInput("Maria", null, "invalido", null)));
		verifyNoInteractions(repo);
	}

}
