package com.klaus.moply.payments.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.usecase.GetCollaboratorPaymentSummary;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

class GetCollaboratorPaymentSummaryTest {

	@Test
	void shouldFlagOnlyCompletedWorkWhoseDateHasArrivedAndHasRemainingAmount() {
		var organizationId = UUID.randomUUID();
		var collaboratorId = UUID.randomUUID();
		var collaboratorRepository = mock(CollaboratorRepository.class);
		var workOrders = mock(WorkOrderRepository.class);
		var payments = mock(CollaboratorPaymentRepository.class);
		var organizations = mock(OrganizationRepository.class);
		when(collaboratorRepository.findById(organizationId, collaboratorId))
			.thenReturn(Optional.of(Collaborator.create(organizationId, "Former teammate", null).deactivate()));
		when(organizations.findById(organizationId))
			.thenReturn(Optional.of(new Organization(organizationId, "Account", "UTC", DefaultWorkStatus.SCHEDULED)));
		var completedToday = work(collaboratorId, LocalDate.of(2026, 10, 3), WorkOrderStatus.COMPLETED);
		var completedFuture = work(collaboratorId, LocalDate.of(2026, 10, 4), WorkOrderStatus.COMPLETED);
		var scheduledPast = work(collaboratorId, LocalDate.of(2026, 10, 2), WorkOrderStatus.SCHEDULED);
		var cancelled = work(collaboratorId, LocalDate.of(2026, 10, 1), WorkOrderStatus.CANCELLED);
		when(workOrders.findAllByCollaborator(organizationId, collaboratorId))
			.thenReturn(List.of(completedToday, completedFuture, scheduledPast, cancelled));
		when(payments.findRecordedTotalsByCollaborator(organizationId, collaboratorId)).thenReturn(
				List.of(new CollaboratorPaymentRepository.RecordedTotal(completedToday.id(), new BigDecimal("10.00"))));

		var usecase = new GetCollaboratorPaymentSummary(workOrders, payments, collaboratorRepository, organizations,
				Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC));

		var result = usecase.execute(new Context(organizationId), collaboratorId);

		assertEquals(3, result.workOrders().size());
		assertEquals(new BigDecimal("138.00"), result.allocatedAmount());
		assertEquals(new BigDecimal("10.00"), result.recordedAmount());
		assertEquals(new BigDecimal("128.00"), result.remainingAmount());
		assertTrue(result.requiresAttention());
		assertTrue(result.workOrders().get(0).requiresAttention());
		assertFalse(result.workOrders().get(1).requiresAttention());
		assertFalse(result.workOrders().get(2).requiresAttention());
	}

	private WorkOrder work(UUID collaboratorId, LocalDate date, WorkOrderStatus status) {
		var draft = WorkOrder.create(UUID.randomUUID(), null, date, null, null, new BigDecimal("4.00"),
				new BigDecimal("11.50"), List.of(collaboratorId),
				status == WorkOrderStatus.CANCELLED ? WorkOrderStatus.SCHEDULED : status);
		return new WorkOrder(UUID.randomUUID(), draft.customerId(), draft.customerLocationId(), draft.serviceDate(),
				draft.startTime(), draft.description(), draft.contractedHours(), draft.hourlyRate(),
				draft.currencyCode(), draft.totalAmount(), draft.allocationPolicyVersion(), status, 0,
				draft.assignments());
	}

}
