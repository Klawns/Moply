package com.klaus.moply.customers.application;

import static com.klaus.moply.factory.AccountFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.CreateCustomer;
import com.klaus.moply.customers.application.usecase.dto.CreateCustomerInput;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationInput;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.factory.CustomerFactory;
import com.klaus.moply.shared.domain.exception.DomainException;

class CreateCustomerTest {

	private final CustomerRepository repo = mock(CustomerRepository.class);

	private final CreateCustomer useCase = new CreateCustomer(repo);

	@Test
	void shouldSaveNameAndReturnAssignedIdentity() {
		UUID id = UUID.randomUUID();

		Customer saved = CustomerFactory.restoreCustomerWithContacts(id);

		when(repo.save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any(Customer.class))).thenReturn(saved);

		var input = new CreateCustomerInput(saved.getName().value(), saved.getPhone().value(), saved.getEmail().value(),
				saved.getNotes(), List.of());

		assertEquals(id, useCase.execute(context(), input));

		ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);

		verify(repo).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), captor.capture());

		assertNull(captor.getValue().getId());
		assertEquals(saved.getName().value(), captor.getValue().getName().value());

		verifyNoMoreInteractions(repo);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "\t\n" })
	void shouldRejectInvalidNameBeforePersistence(String name) {

		var input = new CreateCustomerInput(name, null, null, null, List.of());

		assertThrows(DomainException.class, () -> useCase.execute(context(), input));

		verifyNoInteractions(repo);
	}

	@Test
	void shouldForwardLocationsToCustomer() {
		UUID id = UUID.randomUUID();

		var input = new CreateCustomerInput("Maria", null, null, null,
				List.of(new CustomerLocationInput("Casa", "Rua A, 123", "Portão azul")));

		when(repo.save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any(Customer.class))).thenAnswer(invocation -> {
			Customer customer = invocation.getArgument(1);

			return Customer.restore(id, customer.getName().value(), null, null, null, customer.getLocations());
		});

		assertEquals(id, useCase.execute(context(), input));

		ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);

		verify(repo).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), captor.capture());

		Customer customer = captor.getValue();

		assertEquals(1, customer.getLocations().size());
		assertEquals("Casa", customer.getLocations().get(0).getName());
		assertEquals("Rua A, 123", customer.getLocations().get(0).getAddress());

		verifyNoMoreInteractions(repo);
	}

	@Test
	void shouldRejectInvalidEmailBeforePersistence() {

		var input = new CreateCustomerInput("Maria", null, "invalido", null, List.of());

		assertThrows(DomainException.class, () -> useCase.execute(context(), input));

		verifyNoInteractions(repo);
	}

}
