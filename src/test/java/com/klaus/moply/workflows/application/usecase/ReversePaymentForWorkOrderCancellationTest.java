package com.klaus.moply.workflows.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.payments.domain.PaymentAmount;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReversePaymentForWorkOrderCancellationTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID workId = UUID.randomUUID();

	private final UUID actorId = UUID.randomUUID();

	private final Instant now = Instant.parse("2026-10-05T12:00:00Z");

	private final WorkOrderPaymentRepository payments = mock(WorkOrderPaymentRepository.class);

	private final CollaboratorPaymentRepository settlements = mock(CollaboratorPaymentRepository.class);

	private final ReversePaymentForWorkOrderCancellation usecase = new ReversePaymentForWorkOrderCancellation(payments,
			settlements, Clock.fixed(now, ZoneOffset.UTC));

	@Test
	void shouldCancelUnpaidWorkWithoutConfirmation() {
		usecase.execute(new Context(organizationId), new CancelWorkOrderInput(workId, null, false, null));
		verify(payments, never()).update(any());
	}

	@Test
	void shouldReverseConfirmedPaymentWithActorReasonAndTime() {
		var payment = payment();
		when(payments.findActiveByWork(organizationId, workId)).thenReturn(Optional.of(payment));
		usecase.execute(new Context(organizationId), new CancelWorkOrderInput(workId, actorId, true, "Not received"));
		var captor = ArgumentCaptor.forClass(Payment.class);
		verify(payments).update(captor.capture());
		var reversed = captor.getValue();
		assertEquals(payment.id(), reversed.id());
		assertEquals(Payment.Status.REVERSED, reversed.status());
		assertEquals(actorId, reversed.reversal().by());
		assertEquals("Not received", reversed.reversal().reason());
		assertEquals(now, reversed.reversal().at());
		assertEquals(Payment.Status.RECORDED, payment.status());
	}

	@Test
	void shouldRequireConfirmationAndAuditOnlyWhenPaymentExists() {
		when(payments.findActiveByWork(organizationId, workId)).thenReturn(Optional.of(payment()));
		assertThrows(PaymentConflictException.class, () -> usecase.execute(new Context(organizationId),
				new CancelWorkOrderInput(workId, actorId, false, "reason")));
		assertThrows(DomainException.class, () -> usecase.execute(new Context(organizationId),
				new CancelWorkOrderInput(workId, null, true, "reason")));
		assertThrows(DomainException.class, () -> usecase.execute(new Context(organizationId),
				new CancelWorkOrderInput(workId, actorId, true, " ")));
		verify(payments, never()).update(any());
	}

	@Test
	void shouldRejectSettlementBeforeReversingPayment() {
		when(settlements.hasRecordedForWork(organizationId, workId)).thenReturn(true);
		assertThrows(PaymentConflictException.class, () -> usecase.execute(new Context(organizationId),
				new CancelWorkOrderInput(workId, actorId, true, "reason")));
		verifyNoInteractions(payments);
	}

	private Payment payment() {
		return Payment.create(organizationId, new PaymentAmount(BigDecimal.TEN, "GBP"), LocalDate.of(2026, 10, 5), now,
				actorId);
	}

}
