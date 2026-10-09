package com.klaus.moply.workorders.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.service.WorkOrderPreparation;
import com.klaus.moply.workorders.application.usecase.*;
import com.klaus.moply.workorders.application.usecase.dto.*;
import com.klaus.moply.workorders.application.usecase.exception.PricingAcceptanceException;

class PricingPreviewTest {

	final UUID org = UUID.randomUUID(), customer = UUID.randomUUID(), ana = UUID.randomUUID(),
			bruno = UUID.randomUUID();

	final Context context = new Context(org);

	final WorkOrderRepository orders = mock(WorkOrderRepository.class);

	final CustomerRepository customers = mock(CustomerRepository.class);

	final CollaboratorRepository people = mock(CollaboratorRepository.class);

	final OrganizationRepository organizations = mock(OrganizationRepository.class);

	final WorkOrderPreparation preparation = new WorkOrderPreparation(customers, people, organizations);

	final CreateWorkOrder create = new CreateWorkOrder(orders, customers, preparation);

	@BeforeEach
	void setup() {
		when(organizations.findById(org)).thenReturn(
				Optional.of(new Organization(org, "Test", "UTC", DefaultWorkStatus.SCHEDULED, new BigDecimal("30"))));
		when(customers.findById(org, customer)).thenReturn(Optional.of(Customer.restore(customer, "Client")));
		when(people.findById(org, ana))
			.thenReturn(Optional.of(Collaborator.restore(ana, org, "Ana", null, true, 0, new BigDecimal("20"))));
		when(people.findById(org, bruno))
			.thenReturn(Optional.of(Collaborator.restore(bruno, org, "Bruno", null, true, 0)));
		when(orders.save(eq(org), any())).thenAnswer(i -> i.getArgument(1));
	}

	CreateWorkOrderInput input(BigDecimal rate, String fingerprint) {
		return new CreateWorkOrderInput(customer, null, LocalDate.of(2026, 10, 5), null, null, new BigDecimal("4"),
				rate, List.of(ana, bruno), null, fingerprint);
	}

	@Test
	void shouldPreviewWithoutSavingAndRequireAcceptance() {
		var preview = new PreviewWorkOrderPricing(preparation).execute(context, input(null, null));
		assertEquals(new BigDecimal("30.00"), preview.hourlyRate());
		assertEquals(new BigDecimal("50.00"), preview.participants().getFirst().allocatedAmount());
		verifyNoInteractions(orders);
		assertThrows(PricingAcceptanceException.class, () -> create.execute(context, input(null, null)));
		verifyNoInteractions(orders);
		var result = create.execute(context, input(null, preview.pricingFingerprint()));
		assertEquals(new BigDecimal("70.00"), result.assignments().getLast().allocatedAmount());
	}

	@Test
	void shouldRejectAcceptanceAfterRateChanges() {
		var preview = new PreviewWorkOrderPricing(preparation).execute(context, input(null, null));
		when(people.findById(org, ana))
			.thenReturn(Optional.of(Collaborator.restore(ana, org, "Ana", null, true, 1, new BigDecimal("25"))));
		assertThrows(PricingAcceptanceException.class,
				() -> create.execute(context, input(null, preview.pricingFingerprint())));
		verifyNoInteractions(orders);
	}

	@Test
	void shouldUseExplicitRateAndBlockExcessEvenWithAcceptance() {
		var preview = new PreviewWorkOrderPricing(preparation).execute(context, input(BigDecimal.TEN, null));
		assertFalse(preview.canCreate());
		assertEquals(new BigDecimal("20.00"), preview.excessAmount());
		assertThrows(PricingAcceptanceException.class,
				() -> create.execute(context, input(BigDecimal.TEN, preview.pricingFingerprint())));
		verifyNoInteractions(orders);
	}

	@Test
	void shouldRequireRateWhenPreferenceIsAbsent() {
		when(organizations.findById(org))
			.thenReturn(Optional.of(new Organization(org, "Test", "UTC", DefaultWorkStatus.SCHEDULED)));
		assertThrows(DomainException.class,
				() -> new PreviewWorkOrderPricing(preparation).execute(context, input(null, null)));
	}

	@Test
	void shouldInvalidateAcceptanceWhenParticipantOrderOrCreationDataChanges() {
		var preview = new PreviewWorkOrderPricing(preparation).execute(context, input(null, null));
		var changed = new CreateWorkOrderInput(customer, null, LocalDate.of(2026, 10, 6), null, null,
				new BigDecimal("4"), null, List.of(ana, bruno), null, preview.pricingFingerprint());
		assertThrows(PricingAcceptanceException.class, () -> create.execute(context, changed));
		var reordered = new CreateWorkOrderInput(customer, null, LocalDate.of(2026, 10, 5), null, null,
				new BigDecimal("4"), null, List.of(bruno, ana), null, preview.pricingFingerprint());
		assertThrows(PricingAcceptanceException.class, () -> create.execute(context, reordered));
		verifyNoInteractions(orders);
	}

}
