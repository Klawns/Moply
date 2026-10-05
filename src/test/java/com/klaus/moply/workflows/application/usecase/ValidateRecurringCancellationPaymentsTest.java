package com.klaus.moply.workflows.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.payments.domain.PaymentAmount;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation;
import com.klaus.moply.workflows.application.usecase.dto.ValidateRecurringCancellationPaymentsInput;
import com.klaus.moply.workflows.application.usecase.support.CancellationConfirmations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidateRecurringCancellationPaymentsTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID workId = UUID.randomUUID();

	private final UUID actorId = UUID.randomUUID();

	private final Instant now = Instant.parse("2026-10-05T12:00:00Z");

	private final WorkOrderPaymentRepository payments = mock(WorkOrderPaymentRepository.class);

	private final CollaboratorPaymentRepository settlements = mock(CollaboratorPaymentRepository.class);

	private final ValidateRecurringCancellationPayments usecase = new ValidateRecurringCancellationPayments(payments,
			settlements);

	@Test
	void shouldMatchConfirmationsByPaymentRatherThanListOrder() {
		var otherWork = UUID.randomUUID();
		var firstPayment = payment();
		var secondPayment = payment();
		var first = new PaymentConfirmation(firstPayment.id(), true, "first");
		var second = new PaymentConfirmation(secondPayment.id(), true, "second");
		when(payments.findActiveByWork(organizationId, workId)).thenReturn(Optional.of(firstPayment));
		when(payments.findActiveByWork(organizationId, otherWork)).thenReturn(Optional.of(secondPayment));
		var matched = usecase.execute(new Context(organizationId), new ValidateRecurringCancellationPaymentsInput(
				List.of(workId, otherWork), CancellationConfirmations.from(List.of(second, first))));
		assertEquals(first, matched.get(workId));
		assertEquals(second, matched.get(otherWork));
		assertThrows(UnsupportedOperationException.class, matched::clear);
		verify(payments, never()).update(any());
	}

	@Test
	void shouldRejectMissingUnknownAndDuplicateConfirmations() {
		var payment = payment();
		var confirmation = new PaymentConfirmation(payment.id(), true, "reason");
		when(payments.findActiveByWork(organizationId, workId)).thenReturn(Optional.of(payment));
		assertThrows(PaymentConflictException.class,
				() -> usecase.execute(new Context(organizationId), new ValidateRecurringCancellationPaymentsInput(
						List.of(workId), CancellationConfirmations.from(List.of()))));
		assertThrows(PaymentConflictException.class,
				() -> usecase.execute(new Context(organizationId), new ValidateRecurringCancellationPaymentsInput(
						List.of(), CancellationConfirmations.from(List.of(confirmation)))));
		assertThrows(DomainException.class, () -> CancellationConfirmations.from(List.of(confirmation, confirmation)));
		verify(payments, never()).update(any());
	}

	@Test
	void shouldRejectSettlementBeforeReadingActivePayment() {
		when(settlements.hasRecordedForWork(organizationId, workId)).thenReturn(true);
		assertThrows(PaymentConflictException.class,
				() -> usecase.execute(new Context(organizationId), new ValidateRecurringCancellationPaymentsInput(
						List.of(workId), CancellationConfirmations.from(List.of()))));
		verifyNoInteractions(payments);
	}

	@Test
	void shouldAllowUnpaidWorkWithoutConfirmations() {
		var result = usecase.execute(new Context(organizationId), new ValidateRecurringCancellationPaymentsInput(
				List.of(workId), CancellationConfirmations.from(List.of())));
		assertTrue(result.isEmpty());
		verify(settlements).hasRecordedForWork(organizationId, workId);
		verify(payments).findActiveByWork(organizationId, workId);
		verify(payments, never()).update(any());
	}

	private Payment payment() {
		return Payment.create(organizationId, new PaymentAmount(BigDecimal.TEN, "GBP"), LocalDate.of(2026, 10, 5), now,
				actorId);
	}

}
