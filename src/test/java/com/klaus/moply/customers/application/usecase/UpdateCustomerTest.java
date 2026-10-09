package com.klaus.moply.customers.application.usecase;

import static com.klaus.moply.factory.AccountFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.UpdateCustomerInput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.shared.domain.exception.DomainException;

class UpdateCustomerTest {

	private final CustomerRepository repo = mock(CustomerRepository.class);

	private final UpdateCustomer useCase = new UpdateCustomer(repo);

	private final UUID id = UUID.randomUUID();

	@Test
	void shouldUpdateAndClearOptionalFieldsPreservingIdentity() {
		when(repo.findById(ACCOUNT, id)).thenReturn(Optional.of(Customer.restore(id, "Maria", "123", "a@b", "nota")));
		when(repo.save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any()))
			.thenAnswer(invocation -> invocation.getArgument(1));
		var output = useCase.execute(context(), new UpdateCustomerInput(id, "  Ana  ", " 456 ", null, " "));
		assertEquals(id, output.id());
		assertEquals("Ana", output.name());
		assertEquals("456", output.phone());
		assertNull(output.email());
		assertNull(output.notes());
	}

	@Test
	void shouldRejectMissingCustomerWithoutSaving() {
		assertThrows(CustomerNotFoundException.class,
				() -> useCase.execute(context(), new UpdateCustomerInput(id, "Ana", null, null, null)));
		verify(repo, never()).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any());
	}

	@Test
	void shouldRejectInvalidUpdateWithoutSaving() {
		when(repo.findById(ACCOUNT, id)).thenReturn(Optional.of(Customer.restore(id, "Maria")));
		assertThrows(DomainException.class,
				() -> useCase.execute(context(), new UpdateCustomerInput(id, "Ana", null, "invalid", null)));
		verify(repo, never()).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any());
	}

}
