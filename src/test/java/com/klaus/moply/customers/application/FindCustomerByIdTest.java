package com.klaus.moply.customers.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.FindCustomerById;
import com.klaus.moply.customers.application.usecase.dto.CustomerOutput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.factory.CustomerFactory;

class FindCustomerByIdTest {

	private final CustomerRepository repo = mock(CustomerRepository.class);

	private final FindCustomerById useCase = new FindCustomerById(repo);

	@Test
	void shouldMapExistingCustomer() {
		UUID id = UUID.randomUUID();
		Customer customer = CustomerFactory.restoreCustomer(id);
		when(repo.findById(id)).thenReturn(Optional.of(customer));
		CustomerOutput output = useCase.execute(id);
		assertEquals(id, output.id());
		assertEquals(customer.getName().value(), output.name());
		assertNull(output.phone());
		assertNull(output.email());
		assertNull(output.notes());
		verify(repo).findById(id);
		verifyNoMoreInteractions(repo);
	}

	@Test
	void shouldThrowWhenCustomerDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repo.findById(id)).thenReturn(Optional.empty());
		CustomerNotFoundException error = assertThrows(CustomerNotFoundException.class, () -> useCase.execute(id));
		assertTrue(error.getMessage().contains(id.toString()));
		verify(repo).findById(id);
		verifyNoMoreInteractions(repo);
	}

	@Test
	void shouldMapAllOptionalFields() {
		UUID id = UUID.randomUUID();
		Customer customer = CustomerFactory.restoreCustomerWithContacts(id);
		when(repo.findById(id)).thenReturn(Optional.of(customer));
		CustomerOutput output = useCase.execute(id);
		assertEquals(id, output.id());
		assertEquals(customer.getName().value(), output.name());
		assertEquals(customer.getPhone().value(), output.phone());
		assertEquals(customer.getEmail().value(), output.email());
		assertEquals(customer.getNotes(), output.notes());
		verify(repo).findById(id);
		verifyNoMoreInteractions(repo);
	}

}
