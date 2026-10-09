package com.klaus.moply.workflows.application.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.workflows.application.usecase.dto.CancelOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.CancelSelectedWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.OccurrenceSelection;
import com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleSelectedWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleWorkOrderInput;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences.Reference;
import com.klaus.moply.workorders.application.usecase.FindWorkOccurrence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SelectedWorkOrderTest {

	private final UUID id = UUID.randomUUID();

	private final UUID actor = UUID.randomUUID();

	private final Context context = new Context(UUID.randomUUID());

	private final FindWorkOccurrence occurrence = mock(FindWorkOccurrence.class);

	private final CancelWorkOrder cancel = mock(CancelWorkOrder.class);

	private final CancelRecurringWork cancelRecurring = mock(CancelRecurringWork.class);

	private final RescheduleWorkOrder reschedule = mock(RescheduleWorkOrder.class);

	private final RescheduleRecurringWork rescheduleRecurring = mock(RescheduleRecurringWork.class);

	private final CancelSelectedWorkOrder selectedCancel = new CancelSelectedWorkOrder(occurrence, cancel,
			cancelRecurring);

	private final RescheduleSelectedWorkOrder selectedReschedule = new RescheduleSelectedWorkOrder(occurrence,
			reschedule, rescheduleRecurring);

	private void reference(boolean recurring) {
		when(occurrence.execute(context, id)).thenReturn(
				new Reference(id, recurring ? UUID.randomUUID() : null, recurring ? LocalDate.of(2026, 10, 5) : null));
	}

	@Test
	void shouldCancelStandaloneWithoutBody() {
		reference(false);
		selectedCancel.execute(context, new CancelSelectedWorkOrderInput(id, actor, null));
		verify(cancel).execute(context, new CancelWorkOrderInput(id, actor, false, null));
		verifyNoInteractions(cancelRecurring);
	}

	@Test
	void shouldRouteRecurringCancellationWithActorAndConfirmations() {
		reference(true);
		var confirmations = List.of(new PaymentConfirmation(UUID.randomUUID(), true, "estorno"));
		selectedCancel.execute(context,
				new CancelSelectedWorkOrderInput(id, actor, new CancelSelectedWorkOrderInput.Options(null, null,
						ChangeScope.THIS_AND_FOLLOWING, "key", confirmations)));
		verify(cancelRecurring).execute(context, new CancelOccurrenceInput(
				new OccurrenceSelection(id, actor, ChangeScope.THIS_AND_FOLLOWING, "key"), confirmations));
		verifyNoInteractions(cancel);
	}

	@Test
	void shouldRequireRecurringOptionsAndValidKeys() {
		reference(true);
		assertThrows(ApplicationException.class,
				() -> selectedCancel.execute(context, new CancelSelectedWorkOrderInput(id, actor, null)));
		for (String key : new String[] { null, "", "  ", "x".repeat(256) }) {
			assertThrows(ApplicationException.class, () -> selectedCancel
				.execute(context, new CancelSelectedWorkOrderInput(id, actor,
						new CancelSelectedWorkOrderInput.Options(null, null, ChangeScope.THIS_OCCURRENCE, key, null))));
			assertThrows(ApplicationException.class,
					() -> selectedReschedule.execute(context, new RescheduleSelectedWorkOrderInput(id, actor,
							LocalDate.now(), null, ChangeScope.THIS_OCCURRENCE, key)));
		}
		assertThrows(ApplicationException.class, () -> selectedReschedule.execute(context,
				new RescheduleSelectedWorkOrderInput(id, actor, LocalDate.now(), null, null, "key")));
		verifyNoInteractions(cancel, cancelRecurring, reschedule, rescheduleRecurring);
	}

	@Test
	void shouldRejectCollectiveScopeForStandaloneOperations() {
		reference(false);
		assertThrows(ApplicationException.class, () -> selectedCancel
			.execute(context, new CancelSelectedWorkOrderInput(id, actor,
					new CancelSelectedWorkOrderInput.Options(null, null, ChangeScope.THIS_AND_FOLLOWING, null, null))));
		assertThrows(ApplicationException.class,
				() -> selectedReschedule.execute(context, new RescheduleSelectedWorkOrderInput(id, actor,
						LocalDate.now(), null, ChangeScope.THIS_AND_FOLLOWING, null)));
		verifyNoInteractions(cancel, cancelRecurring, reschedule, rescheduleRecurring);
	}

	@Test
	void shouldRouteReschedulingByOccurrenceIdentity() {
		var date = LocalDate.of(2026, 10, 6);
		reference(false);
		selectedReschedule.execute(context, new RescheduleSelectedWorkOrderInput(id, actor, date, null, null, null));
		verify(reschedule).execute(context, new RescheduleWorkOrderInput(id, date, null));
		reference(true);
		selectedReschedule.execute(context,
				new RescheduleSelectedWorkOrderInput(id, actor, date, null, ChangeScope.THIS_OCCURRENCE, "key"));
		verify(rescheduleRecurring).execute(context, new RescheduleOccurrenceInput(
				new OccurrenceSelection(id, actor, ChangeScope.THIS_OCCURRENCE, "key"), date, null));
	}

}
