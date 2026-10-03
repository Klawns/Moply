package com.klaus.moply.workorders.application;

import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.exception.CustomerLocationNotFoundException;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.*;
import com.klaus.moply.workorders.application.usecase.dto.*;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;
import com.klaus.moply.workorders.domain.entity.*;

class WorkOrderUsecasesTest {

	final UUID account = UUID.randomUUID(), customer = UUID.randomUUID(), person = UUID.randomUUID();

	final Context context = new Context(account);

	final WorkOrderRepository orders = mock(WorkOrderRepository.class);

	final CustomerRepository customers = mock(CustomerRepository.class);

	final CollaboratorRepository people = mock(CollaboratorRepository.class);

	final OrganizationRepository accounts = mock(OrganizationRepository.class);

	final CreateWorkOrder create = new CreateWorkOrder(orders, customers, people, accounts);

	CreateWorkOrderInput input(UUID location, WorkOrderStatus status) {
		return new CreateWorkOrderInput(customer, location, LocalDate.of(2026, 9, 28), LocalTime.of(10, 30),
				"Description", BigDecimal.ONE, BigDecimal.TEN, List.of(person), status);
	}

	@BeforeEach
	void setup() {
		when(accounts.findById(account))
			.thenReturn(Optional.of(new Organization(account, "Account", "UTC", DefaultWorkStatus.COMPLETED)));
		when(customers.findById(account, customer)).thenReturn(Optional.of(Customer.restore(customer, "Current name")));
		when(people.findById(account, person))
			.thenReturn(Optional.of(Collaborator.restore(person, account, "Worker", null, true, 0)));
		when(orders.save(eq(account), any())).thenAnswer(i -> i.getArgument(1));
	}

	@Test
	void shouldUseAccountPreferenceOrExplicitInitialState() {
		assertEquals(WorkOrderStatus.COMPLETED, create.execute(context, input(null, null)).status());
		var explicit = create.execute(context, input(null, WorkOrderStatus.SCHEDULED));
		assertEquals(WorkOrderStatus.SCHEDULED, explicit.status());
		assertEquals("Current name", explicit.customer());
		assertEquals(LocalTime.of(10, 30), explicit.startTime());
	}

	@Test
	void shouldRejectForeignOrMissingCustomerAndLocation() {
		assertThrows(CustomerLocationNotFoundException.class,
				() -> create.execute(context, input(UUID.randomUUID(), null)));
		when(customers.findById(account, customer)).thenReturn(Optional.empty());
		assertThrows(CustomerNotFoundException.class, () -> create.execute(context, input(null, null)));
		verifyNoInteractions(orders);
	}

	@Test
	void shouldRejectMissingForeignAndInactiveParticipants() {
		when(people.findById(account, person)).thenReturn(Optional.empty());
		assertThrows(CollaboratorNotFoundException.class, () -> create.execute(context, input(null, null)));
		when(people.findById(account, person))
			.thenReturn(Optional.of(Collaborator.restore(person, account, "Worker", null, false, 1)));
		assertThrows(InactiveCollaboratorException.class, () -> create.execute(context, input(null, null)));
		verifyNoInteractions(orders);
	}

	@Test
	void shouldReadStoredWorkWithoutConsultingPreferenceOrParticipantState() {
		var work = WorkOrder.create(customer, null, LocalDate.now(), null, null, BigDecimal.ONE, BigDecimal.TEN,
				List.of(person), WorkOrderStatus.SCHEDULED);
		UUID id = UUID.randomUUID();
		when(orders.findById(account, id)).thenReturn(Optional.of(work));
		var output = new FindWorkOrderById(orders, customers).execute(context, id);
		assertEquals("Current name", output.customer());
		assertEquals(WorkOrderStatus.SCHEDULED, output.status());
		verifyNoInteractions(accounts, people);
		assertThrows(WorkOrderNotFoundException.class,
				() -> new FindWorkOrderById(orders, customers).execute(context, UUID.randomUUID()));
	}

	@Test
	void shouldCombineFiltersAndRejectInvertedDates() {
		var date = LocalDate.now();
		var filter = new FindWorkOrders.Filter(date, date, customer);
		when(orders.findAll(account, new WorkOrderDateRange(date, date), customer, null)).thenReturn(List.of());
		assertTrue(new FindWorkOrders(orders, customers).execute(context, filter).isEmpty());
		verify(orders).findAll(account, new WorkOrderDateRange(date, date), customer, null);
		assertThrows(DomainException.class, () -> new FindWorkOrders.Filter(date, date.minusDays(1), null));
	}

}
