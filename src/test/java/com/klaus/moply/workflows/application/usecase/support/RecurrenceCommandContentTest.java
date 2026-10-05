package com.klaus.moply.workflows.application.usecase.support;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.workflows.application.usecase.dto.CancelOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.OccurrenceSelection;
import com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleOccurrenceInput;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecurrenceCommandContentTest {

	private final UUID workId = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private final UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private final UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000000003");

	private final OccurrenceSelection selection = new OccurrenceSelection(workId, actorId,
			ChangeScope.THIS_AND_FOLLOWING, "key");

	@Test
	void shouldPreservePersistedCancellationContent() {
		var input = new CancelOccurrenceInput(selection, List.of(new PaymentConfirmation(paymentId, true, " reason ")));
		assertEquals(
				"Q0FOQ0VM.MDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAx.MDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAy.VEhJU19BTkRfRk9MTE9XSU5H.MDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAz.dHJ1ZQ==.cmVhc29u",
				RecurrenceCommandContent.cancellationContent(input));
	}

	@Test
	void shouldPreservePersistedReschedulingContentWithNullStartTime() {
		var input = new RescheduleOccurrenceInput(selection, LocalDate.of(2026, 10, 6), null);
		assertEquals(
				"UkVTQ0hFRFVMRQ==.MDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAx.MDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAy.VEhJU19BTkRfRk9MTE9XSU5H.MjAyNi0xMC0wNg==.bnVsbA==",
				RecurrenceCommandContent.reschedulingContent(input));
	}

	@Test
	void shouldNormalizeConfirmationOrderWithoutChangingInput() {
		var first = new PaymentConfirmation(paymentId, true, "reason");
		var second = new PaymentConfirmation(UUID.randomUUID(), true, "another");
		var forward = new CancelOccurrenceInput(selection, List.of(first, second));
		var reverse = new CancelOccurrenceInput(selection, List.of(second, first));
		assertEquals(RecurrenceCommandContent.cancellationContent(forward),
				RecurrenceCommandContent.cancellationContent(reverse));
		assertEquals(List.of(second, first), reverse.confirmations());
	}

}
