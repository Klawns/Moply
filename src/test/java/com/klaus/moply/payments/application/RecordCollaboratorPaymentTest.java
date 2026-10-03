package com.klaus.moply.payments.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.usecase.RecordCollaboratorPayment;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

class RecordCollaboratorPaymentTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID workOrderId = UUID.randomUUID();

	private final UUID collaboratorId = UUID.randomUUID();

	private final UUID actorId = UUID.randomUUID();

	private final WorkOrderOperations workOrders = mock(WorkOrderOperations.class);

	private final CollaboratorPaymentRepository payments = mock(CollaboratorPaymentRepository.class);

	private final OrganizationRepository organizations = mock(OrganizationRepository.class);

	private final Clock clock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC);

	private final RecordCollaboratorPayment usecase = new RecordCollaboratorPayment(workOrders, payments, organizations,
			clock);

	private WorkOrder work;

	@BeforeEach
	void setUp() {
		work = WorkOrder.create(UUID.randomUUID(), null, LocalDate.of(2026, 10, 3), null, null, new BigDecimal("4.00"),
				new BigDecimal("11.50"), List.of(collaboratorId), WorkOrderStatus.SCHEDULED);
		work = new WorkOrder(workOrderId, work.customerId(), work.customerLocationId(), work.serviceDate(),
				work.startTime(), work.description(), work.contractedHours(), work.hourlyRate(), work.currencyCode(),
				work.totalAmount(), work.allocationPolicyVersion(), work.status(), work.version(), work.assignments());
		when(organizations.findById(organizationId))
			.thenReturn(Optional.of(new Organization(organizationId, "Test", "UTC", DefaultWorkStatus.SCHEDULED)));
		when(workOrders.withWorkOrder(eq(organizationId), eq(workOrderId), any())).thenAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			var operation = (Function<WorkOrder, Object>) invocation.getArgument(2);
			return operation.apply(work);
		});
		when(payments.findByIdempotencyKey(eq(organizationId), anyString())).thenReturn(Optional.empty());
		when(payments.findRecordedTotalsByCollaborator(organizationId, collaboratorId)).thenReturn(List.of());
		when(payments.save(eq(workOrderId), eq(collaboratorId), anyString(), any(Payment.class)))
			.thenAnswer(invocation -> invocation.getArgument(3));
	}

	@Test
	void shouldRecordPartialAmountAndReturnSameRecordForAnIdempotentRetry() {
		var input = new RecordCollaboratorPayment.Input(workOrderId, collaboratorId, new BigDecimal("10.00"),
				LocalDate.of(2026, 10, 3), "request-1", actorId);

		var recorded = usecase.execute(new Context(organizationId), input);
		when(payments.findByIdempotencyKey(organizationId, "request-1")).thenReturn(
				Optional.of(new CollaboratorPaymentRepository.RecordedPayment(workOrderId, collaboratorId, recorded)));

		var retried = usecase.execute(new Context(organizationId), input);

		assertEquals(recorded.id(), retried.id());
		assertEquals(new BigDecimal("10.00"), retried.amount().value());
	}

	@Test
	void shouldRejectAnAmountAboveTheRemainingAllocation() {
		when(payments.findRecordedTotalsByCollaborator(organizationId, collaboratorId))
			.thenReturn(List.of(new CollaboratorPaymentRepository.RecordedTotal(workOrderId, new BigDecimal("20.00"))));

		var input = input(new BigDecimal("27.00"), LocalDate.of(2026, 10, 3), "request-2");

		assertThrows(PaymentConflictException.class, () -> usecase.execute(new Context(organizationId), input));
	}

	@Test
	void shouldRejectAnAdvanceForAFutureWorkOrder() {
		var future = WorkOrder.create(UUID.randomUUID(), null, LocalDate.of(2026, 10, 20), null, null,
				new BigDecimal("4.00"), new BigDecimal("11.50"), List.of(collaboratorId), WorkOrderStatus.SCHEDULED);
		work = new WorkOrder(workOrderId, future.customerId(), null, future.serviceDate(), null, null,
				future.contractedHours(), future.hourlyRate(), future.currencyCode(), future.totalAmount(),
				future.allocationPolicyVersion(), future.status(), future.version(), future.assignments());

		assertThrows(PaymentConflictException.class, () -> usecase.execute(new Context(organizationId),
				input(new BigDecimal("5.00"), LocalDate.of(2026, 10, 3), "advance-1")));
	}

	@Test
	void shouldRejectFutureRecordedDateAndCanceledWork() {
		assertThrows(DomainException.class, () -> usecase.execute(new Context(organizationId),
				input(new BigDecimal("5.00"), LocalDate.of(2026, 10, 4), "future-paid-on")));
		assertThrows(DomainException.class, () -> usecase.execute(new Context(organizationId),
				input(new BigDecimal("5.00"), LocalDate.of(2026, 10, 2), "before-service")));
		work = work.cancel();
		assertThrows(PaymentConflictException.class, () -> usecase.execute(new Context(organizationId),
				input(new BigDecimal("5.00"), LocalDate.of(2026, 10, 3), "cancelled-work")));
	}

	private RecordCollaboratorPayment.Input input(BigDecimal amount, LocalDate date, String key) {
		return new RecordCollaboratorPayment.Input(workOrderId, collaboratorId, amount, date, key, actorId);
	}

}
