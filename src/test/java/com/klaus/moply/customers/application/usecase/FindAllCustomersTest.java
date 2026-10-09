package com.klaus.moply.customers.application.usecase;

import static com.klaus.moply.factory.AccountFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.UUID;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import org.junit.jupiter.api.Test;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.domain.entities.Customer;

class FindAllCustomersTest {

	private final CustomerRepository repo = mock(CustomerRepository.class);

	private final FindAllCustomers useCase = new FindAllCustomers(repo);

	@Test
	void shouldReturnEmptyList() {
		when(repo.findAll(ACCOUNT, PageQuery.defaults())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
		assertTrue(useCase.execute(context(), PageQuery.defaults()).content().isEmpty());
	}

	@Test
	void shouldKeepCustomersWithSameNameSeparate() {
		var first = Customer.restore(UUID.randomUUID(), "Maria");
		var second = Customer.restore(UUID.randomUUID(), "Maria");
		when(repo.findAll(ACCOUNT, PageQuery.defaults()))
			.thenReturn(new PageResult<>(List.of(first, second), 0, 20, 2, 1));
		var output = useCase.execute(context(), PageQuery.defaults());
		assertEquals(List.of(first.getId(), second.getId()), output.content().stream().map(c -> c.id()).toList());
	}

}
