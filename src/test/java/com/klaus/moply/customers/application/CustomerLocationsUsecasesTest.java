package com.klaus.moply.customers.application;

import static com.klaus.moply.factory.AccountFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.*;
import com.klaus.moply.customers.application.usecase.dto.*;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.customers.domain.exception.CustomerLocationNotFoundException;
import com.klaus.moply.shared.domain.exception.DomainException;

import org.mockito.ArgumentCaptor;

class CustomerLocationsUsecasesTest {

	private final CustomerRepository repo = mock(CustomerRepository.class);

	private final UUID id = UUID.randomUUID();

	private Customer existingCustomer() {
		var customer = Customer.restore(id, "Maria", "123", "a@b", "nota",
				List.of(CustomerLocation.create("Casa", "Rua 1", null)));
		when(repo.findById(ACCOUNT, id)).thenReturn(Optional.of(customer));
		return customer;
	}

	@Test
	void shouldCreateCustomerAndLocationsInOneSave() {
		when(repo.save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any())).thenAnswer(invocation -> {
			Customer customer = invocation.getArgument(1);
			return Customer.restore(id, customer.getName().value(),
					customer.getPhone() == null ? null : customer.getPhone().value(),
					customer.getEmail() == null ? null : customer.getEmail().value(), customer.getNotes(),
					customer.getLocations());
		});
		var input = new CreateCustomerInput("Maria", null, null, null,
				List.of(new CustomerLocationInput("Casa", null, null),
						new CustomerLocationInput("Escritório", "Rua 2", "nota")));
		assertEquals(id, new CreateCustomer(repo).execute(context(), input));
		var captor = ArgumentCaptor.forClass(Customer.class);
		verify(repo).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), captor.capture());
		var locations = captor.getValue().getLocations();
		assertEquals(2, locations.size());
		assertNotEquals(locations.getFirst().getId(), locations.get(1).getId());
		assertNull(locations.getFirst().getAddress());
		assertEquals("Rua 2", locations.get(1).getAddress());
		verifyNoMoreInteractions(repo);
	}

	@Test
	void shouldRejectInvalidInitialLocationBeforeSavingCustomer() {
		var input = new CreateCustomerInput("Maria", null, null, null,
				List.of(new CustomerLocationInput("Casa", null, null), new CustomerLocationInput(" ", null, null)));
		assertThrows(DomainException.class, () -> new CreateCustomer(repo).execute(context(), input));
		verifyNoInteractions(repo);
	}

	@Test
	void shouldAddLocationPreservingExistingLocationsAndCustomerFields() {
		var original = existingCustomer();
		when(repo.save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any()))
			.thenAnswer(invocation -> invocation.getArgument(1));
		var output = new AddCustomerLocation(repo).execute(context(),
				new AddCustomerLocationInput(id, " Escritório ", " Rua 2 ", null));
		var captor = ArgumentCaptor.forClass(Customer.class);
		verify(repo).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), captor.capture());
		var saved = captor.getValue();
		assertEquals(id, saved.getId());
		assertEquals("Maria", saved.getName().value());
		assertEquals("123", saved.getPhone().value());
		assertEquals(original.getLocations().getFirst(), saved.getLocations().getFirst());
		assertEquals(output.id(), saved.getLocations().get(1).getId());
		assertEquals("Escritório", output.name());
		assertEquals("Rua 2", output.address());
	}

	@Test
	void shouldRejectInvalidAddedLocationWithoutSaving() {
		existingCustomer();
		assertThrows(DomainException.class, () -> new AddCustomerLocation(repo).execute(context(),
				new AddCustomerLocationInput(id, " ", null, null)));
		verify(repo, never()).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any());
	}

	@Test
	void shouldUpdateLocationWithinCustomer() {
		var customer = existingCustomer();
		var locationId = customer.getLocations().getFirst().getId();
		when(repo.save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any()))
			.thenAnswer(invocation -> invocation.getArgument(1));
		var output = new UpdateCustomerLocation(repo).execute(context(),
				new UpdateCustomerLocationInput(id, locationId, "Nova casa", null, "nova nota"));
		assertEquals(locationId, output.id());
		assertEquals("Nova casa", output.name());
		assertNull(output.address());
		assertEquals("nova nota", output.notes());
	}

	@Test
	void shouldRejectInvalidLocationUpdateWithoutSaving() {
		var customer = existingCustomer();
		assertThrows(DomainException.class, () -> new UpdateCustomerLocation(repo).execute(context(),
				new UpdateCustomerLocationInput(id, customer.getLocations().getFirst().getId(), " ", null, null)));
		verify(repo, never()).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any());
	}

	@Test
	void shouldRejectLookupAndUpdateOfAnotherCustomersLocation() {
		existingCustomer();
		var otherCustomer = Customer.create("Ana").addLocation(CustomerLocation.create("Casa", null, null));
		UUID otherLocationId = otherCustomer.getLocations().getFirst().getId();
		assertThrows(CustomerLocationNotFoundException.class, () -> new FindCustomerLocationById(repo)
			.execute(context(), new FindCustomerLocationByIdInput(id, otherLocationId)));
		assertThrows(CustomerLocationNotFoundException.class, () -> new UpdateCustomerLocation(repo).execute(context(),
				new UpdateCustomerLocationInput(id, otherLocationId, "Casa", null, null)));
		verify(repo, never()).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any());
	}

	@Test
	void shouldRejectAllLocationOperationsForMissingCustomer() {
		assertThrows(CustomerNotFoundException.class, () -> new AddCustomerLocation(repo).execute(context(),
				new AddCustomerLocationInput(id, "Casa", null, null)));
		assertThrows(CustomerNotFoundException.class, () -> new UpdateCustomerLocation(repo).execute(context(),
				new UpdateCustomerLocationInput(id, UUID.randomUUID(), "Casa", null, null)));
		assertThrows(CustomerNotFoundException.class, () -> new FindCustomerLocationById(repo).execute(context(),
				new FindCustomerLocationByIdInput(id, UUID.randomUUID())));
		assertThrows(CustomerNotFoundException.class, () -> new FindCustomerLocations(repo).execute(context(), id));
		verify(repo, never()).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any());
	}

	@Test
	void shouldReturnLocationsThroughCustomerAndLocationQueries() {
		var customer = existingCustomer();
		var location = customer.getLocations().getFirst();
		var expected = CustomerLocationOutput.fromDomain(location);
		assertEquals(expected, new FindCustomerLocationById(repo).execute(context(),
				new FindCustomerLocationByIdInput(id, location.getId())));
		assertEquals(List.of(expected), new FindCustomerLocations(repo).execute(context(), id));
		assertEquals(List.of(expected), new FindCustomerById(repo).execute(context(), id).locations());
		when(repo.findAll(ACCOUNT)).thenReturn(List.of(customer));
		assertEquals(List.of(expected), new FindAllCustomers(repo).execute(context(), null).getFirst().locations());
		verify(repo, never()).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any());
	}

	@Test
	void shouldReturnEmptyLocationsForCustomerWithoutLocations() {
		when(repo.findById(ACCOUNT, id)).thenReturn(Optional.of(Customer.restore(id, "Maria")));
		assertTrue(new FindCustomerLocations(repo).execute(context(), id).isEmpty());
	}

	@Test
	void shouldPreserveLocationsWhenUpdatingCustomer() {
		var customer = existingCustomer();
		when(repo.save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any()))
			.thenAnswer(invocation -> invocation.getArgument(1));
		var output = new UpdateCustomer(repo).execute(context(), new UpdateCustomerInput(id, "Ana", null, null, null));
		assertEquals(customer.getLocations().getFirst().getId(), output.locations().getFirst().id());
	}

	@Test
	void shouldRejectNullLocationIdWithoutSaving() {
		existingCustomer();
		assertThrowsExactly(DomainException.class, () -> new FindCustomerLocationById(repo).execute(context(),
				new FindCustomerLocationByIdInput(id, null)));
		assertThrowsExactly(DomainException.class, () -> new UpdateCustomerLocation(repo).execute(context(),
				new UpdateCustomerLocationInput(id, null, "Casa", null, null)));
		verify(repo, never()).save(org.mockito.ArgumentMatchers.eq(ACCOUNT), any());
	}

}
