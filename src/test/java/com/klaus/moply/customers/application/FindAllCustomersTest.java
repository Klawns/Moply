package com.klaus.moply.customers.application;

import static com.klaus.moply.factory.AccountFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.FindAllCustomers;
import com.klaus.moply.customers.domain.entities.Customer;

class FindAllCustomersTest {

	private final CustomerRepository repo = mock(CustomerRepository.class);

	private final FindAllCustomers useCase = new FindAllCustomers(repo);

	@Test
	void shouldReturnEmptyList() {
		when(repo.findAll(ACCOUNT)).thenReturn(List.of());
		assertTrue(useCase.execute(context(), null).isEmpty());
	}

	@Test
	void shouldKeepCustomersWithSameNameSeparate() {
		var first = Customer.restore(UUID.randomUUID(), "Maria");
		var second = Customer.restore(UUID.randomUUID(), "Maria");
		when(repo.findAll(ACCOUNT)).thenReturn(List.of(first, second));
		var output = useCase.execute(context(), null);
		assertEquals(List.of(first.getId(), second.getId()), output.stream().map(c -> c.id()).toList());
	}

}
