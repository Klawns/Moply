package com.klaus.moply.workflows.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.klaus.moply.accounts.application.usecase.GetOrganizationDate;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.payments.domain.PaymentAmount;
import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.application.usecase.GenerateSeries;
import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.recurrence.domain.Frequency;
import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import com.klaus.moply.recurrence.domain.WorkTemplate;
import com.klaus.moply.recurrence.domain.vo.RecurrenceParticipants;
import com.klaus.moply.recurrence.domain.vo.RecurrencePeriod;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.CancelOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.OccurrenceSelection;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.SelectedWork;
import com.klaus.moply.workflows.application.usecase.dto.SelectionContext;
import com.klaus.moply.workflows.application.usecase.support.RecurrenceCommandContent;
import com.klaus.moply.workflows.application.usecase.support.RecurringWorkSelection;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import com.klaus.moply.workorders.domain.vo.WorkOrderSchedule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowsTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID actorId = UUID.randomUUID();

	private final Context context = new Context(organizationId);

	private final LocalDate today = LocalDate.of(2026, 10, 5);

	private final Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);

	private final RecurrenceRepository series = mock(RecurrenceRepository.class);

	private final RecurrenceChanges changes = mock(RecurrenceChanges.class);

	private final WorkOrderOperations operations = mock(WorkOrderOperations.class);

	private final WorkOrderPaymentRepository payments = mock(WorkOrderPaymentRepository.class);

	private final CollaboratorPaymentRepository settlements = mock(CollaboratorPaymentRepository.class);

	private final RecurringWorkSelection selection = mock(RecurringWorkSelection.class);

	private final GetOrganizationDate dates = mock(GetOrganizationDate.class);

	private final GenerateSeries generate = mock(GenerateSeries.class);

	private final CheckRecurrenceCommandReplay replay = new CheckRecurrenceCommandReplay(changes);

	private final RecordRecurrenceCommand recordCommand = new RecordRecurrenceCommand(changes, clock);

	private final ReversePaymentForWorkOrderCancellation paymentRules = new ReversePaymentForWorkOrderCancellation(
			payments, settlements, clock);

	private final ValidateRecurringCancellationPayments collectivePayments = new ValidateRecurringCancellationPayments(
			payments, settlements);

	private final CancelWorkOrder cancel = new CancelWorkOrder(operations, paymentRules);

	private final RescheduleWorkOrder reschedule = new RescheduleWorkOrder(operations, payments, dates);

	private final CancelRecurringWork cancelRecurring = new CancelRecurringWork(selection, replay, recordCommand,
			changes, cancel, collectivePayments, series);

	private final RescheduleRecurringWork rescheduleRecurring = new RescheduleRecurringWork(selection, replay,
			recordCommand, changes, reschedule, cancel, series, generate, payments, settlements, dates);

	private final Map<UUID, WorkOrder> storedWorks = new HashMap<>();

	private final WorkOrder work = persistedWork();

	private final RecurrenceSeries anchor = RecurrenceSeries.create(organizationId, Frequency.WEEKLY,
			new RecurrencePeriod(today, null),
			new WorkTemplate(work.customerId(), null, null, null, new DurationHours(BigDecimal.ONE),
					new HourlyRate(BigDecimal.TEN), "GBP",
					new RecurrenceParticipants(
							work.assignments().stream().map(assignment -> assignment.collaboratorId()).toList()),
					WorkOrderStatus.SCHEDULED));

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		storedWorks.put(work.id(), work);
		when(dates.execute(context, null)).thenReturn(today);
		doAnswer(invocation -> {
			UUID id = invocation.getArgument(1);
			UnaryOperator<WorkOrder> transition = invocation.getArgument(2);
			storedWorks.put(id, transition.apply(storedWorks.get(id)));
			return null;
		}).when(operations).update(eq(organizationId), any(), any());
	}

	@Test
	void shouldCancelThroughSingleInputAndKeepOriginalEntityImmutable() {
		cancel.execute(context, new CancelWorkOrderInput(work.id(), null, false, null));
		assertEquals(WorkOrderStatus.CANCELLED, storedWorks.get(work.id()).status());
		assertEquals(WorkOrderStatus.SCHEDULED, work.status());
		assertEquals(work.assignments(), storedWorks.get(work.id()).assignments());
	}

	@Test
	void shouldLeaveWorkUnchangedWhenCancellationRequiresPaymentConfirmation() {
		when(payments.findActiveByWork(organizationId, work.id())).thenReturn(Optional.of(payment()));
		assertThrows(PaymentConflictException.class,
				() -> cancel.execute(context, new CancelWorkOrderInput(work.id(), actorId, false, null)));
		assertEquals(work, storedWorks.get(work.id()));
	}

	@Test
	void shouldRescheduleWithoutChangingConditionsAndRejectCancelledWork() {
		reschedule.execute(context, new RescheduleWorkOrderInput(work.id(), today.plusDays(1), null));
		assertEquals(today.plusDays(1), storedWorks.get(work.id()).serviceDate());
		assertEquals(work.assignments(), storedWorks.get(work.id()).assignments());
		cancel.execute(context, new CancelWorkOrderInput(work.id(), null, false, null));
		assertThrows(WorkOrderStateException.class,
				() -> reschedule.execute(context, new RescheduleWorkOrderInput(work.id(), today.plusDays(2), null)));
		assertEquals(today.plusDays(1), storedWorks.get(work.id()).serviceDate());
	}

	@Test
	void shouldRejectInvalidInputsBeforeAnyWorkOrderUpdate() {
		assertThrows(DomainException.class, () -> cancel.execute(context, null));
		assertThrows(DomainException.class, () -> reschedule.execute(context, null));
		assertThrows(DomainException.class, () -> new CancelWorkOrderInput(null, null, false, null));
		assertThrows(DomainException.class, () -> new RescheduleWorkOrderInput(work.id(), null, null));
		verifyNoInteractions(operations);
	}

	@Test
	void shouldReturnReplayBeforeLockingWorksOrChangingFinancialState() {
		var family = selectedFamily(ChangeScope.THIS_AND_FOLLOWING, anchor, List.of());
		var input = new CancelOccurrenceInput(family.selection(), List.of());
		when(changes.find(organizationId, anchor.getId(), "key"))
			.thenReturn(Optional.of(new RecurrenceChanges.Command(UUID.randomUUID(), organizationId, anchor.getId(),
					"key", RecurrenceCommandContent.cancellationContent(input), actorId, clock.instant(), null)));
		cancelRecurring.execute(context, input);
		verify(selection, never()).lockOccurrences(any());
		verifyNoInteractions(payments, settlements, operations, series, generate);
	}

	@Test
	void shouldCancelCollectivelyAndCloseFamilyAfterRecordingChanges() {
		var family = selectedFamily(ChangeScope.THIS_AND_FOLLOWING, anchor, List.of(new SelectedWork(work, 0)));
		cancelRecurring.execute(context, new CancelOccurrenceInput(family.selection(), List.of()));
		assertEquals(WorkOrderStatus.CANCELLED, storedWorks.get(work.id()).status());
		var closed = ArgumentCaptor.forClass(RecurrenceSeries.class);
		verify(series).close(closed.capture());
		assertEquals(0L, closed.getValue().getLineage().untilPosition());
		verify(changes).record(argThat(item -> item.work().equals(work.id()) && item.reason().equals("CANCELLED")));
		verify(changes).exclude(organizationId, anchor.getId(), 0, work.id());
	}

	@Test
	void shouldRescheduleSingleOccurrenceWithoutClosingFamilyOrGeneratingSuccessor() {
		var family = selectedFamily(ChangeScope.THIS_OCCURRENCE, anchor, List.of(new SelectedWork(work, 0)));
		rescheduleRecurring.execute(context,
				new RescheduleOccurrenceInput(family.selection(), today.plusDays(1), null));
		assertEquals(today.plusDays(1), storedWorks.get(work.id()).serviceDate());
		verifyNoInteractions(series, generate);
		verify(changes).record(argThat(item -> item.reason().equals("RESCHEDULED")
				&& item.serviceDateBefore().equals(today) && item.serviceDateAfter().equals(today.plusDays(1))));
	}

	@Test
	void shouldPreserveTerminalFamilyLimitAndHistoricalWorkWhenReplacing() {
		var emptyHistoricalVersion = anchor.successor(10, today, null).closeAt(10);
		var family = selectedFamily(ChangeScope.THIS_AND_FOLLOWING, anchor.closeAt(3),
				List.of(new SelectedWork(work, 0)));
		family = new SelectionContext(family.selection(), organizationId, family.anchor(), 0,
				List.of(family.anchor(), emptyHistoricalVersion));
		when(selection.lockFamily(context, family.selection())).thenReturn(family);
		when(selection.lockOccurrences(family)).thenReturn(List.of(new SelectedWork(work, 0)));
		rescheduleRecurring.execute(context,
				new RescheduleOccurrenceInput(family.selection(), today.plusDays(1), null));
		var successor = ArgumentCaptor.forClass(RecurrenceSeries.class);
		verify(series).save(successor.capture());
		assertEquals(3L, successor.getValue().getLineage().untilPosition());
		assertEquals(today.plusDays(1), successor.getValue().getPeriod().startsOn());
		assertEquals(today, storedWorks.get(work.id()).serviceDate());
		assertEquals(WorkOrderStatus.CANCELLED, storedWorks.get(work.id()).status());
		assertEquals(work.assignments(), storedWorks.get(work.id()).assignments());
		verify(generate).execute(context, successor.getValue().getId());
		verify(changes).record(argThat(item -> item.reason().equals("REPLACED")
				&& item.targetSeries().equals(successor.getValue().getId()) && item.serviceDateBefore().equals(today)));
	}

	@Test
	void shouldBlockCollectiveReplacementBeforeClosingOrRecordingAnything() {
		var family = selectedFamily(ChangeScope.THIS_AND_FOLLOWING, anchor, List.of(new SelectedWork(work, 0)));
		when(payments.findActiveByWork(organizationId, work.id())).thenReturn(Optional.of(payment()));
		assertThrows(PaymentConflictException.class, () -> rescheduleRecurring.execute(context,
				new RescheduleOccurrenceInput(family.selection(), today.plusDays(1), null)));
		assertEquals(work, storedWorks.get(work.id()));
		verifyNoInteractions(series, generate, operations);
		verify(changes, never()).save(any());
		verify(changes, never()).record(any());
	}

	@Test
	void shouldRejectMalformedCancellationBeforeIdempotencyOrLocks() {
		var occurrence = new OccurrenceSelection(work.id(), actorId, ChangeScope.THIS_AND_FOLLOWING, "key");
		var confirmation = new com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation(UUID.randomUUID(),
				true, "reason");
		assertThrows(DomainException.class, () -> cancelRecurring.execute(context, null));
		assertThrows(DomainException.class,
				() -> cancelRecurring.execute(context, new CancelOccurrenceInput(null, null)));
		assertThrows(DomainException.class, () -> cancelRecurring.execute(context,
				new CancelOccurrenceInput(occurrence, java.util.Collections.singletonList(null))));
		assertThrows(DomainException.class, () -> cancelRecurring.execute(context,
				new CancelOccurrenceInput(occurrence, List.of(confirmation, confirmation))));
		assertThrows(DomainException.class, () -> cancelRecurring.execute(context, new CancelOccurrenceInput(occurrence,
				List.of(new com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation(UUID.randomUUID(),
						false, "reason")))));
		verifyNoInteractions(selection, changes, operations, payments, settlements, series);
	}

	@Test
	void shouldKeepInputListImmutableWithoutSortingOrValidatingBusinessRules() {
		var first = new com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation(
				UUID.fromString("00000000-0000-0000-0000-000000000002"), false, null);
		var second = new com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation(
				UUID.fromString("00000000-0000-0000-0000-000000000001"), true, " reason ");
		var confirmations = new java.util.ArrayList<>(List.of(first, second));
		var input = new CancelOccurrenceInput(null, confirmations);
		confirmations.clear();
		assertEquals(List.of(first, second), input.confirmations());
		assertThrows(UnsupportedOperationException.class, () -> input.confirmations().clear());
		assertEquals(List.of(), new CancelOccurrenceInput(null, null).confirmations());
		assertEquals("reason", second.reason());
	}

	@Test
	void shouldPreserveSettlementForSingleReschedulingButBlockReplacement() {
		when(settlements.hasRecordedForWork(organizationId, work.id())).thenReturn(true);
		assertDoesNotThrow(
				() -> reschedule.execute(context, new RescheduleWorkOrderInput(work.id(), today.plusDays(1), null)));
		var family = selectedFamily(ChangeScope.THIS_AND_FOLLOWING, anchor, List.of(new SelectedWork(work, 0)));
		assertThrows(PaymentConflictException.class, () -> rescheduleRecurring.execute(context,
				new RescheduleOccurrenceInput(family.selection(), today.plusDays(2), null)));
		verifyNoInteractions(series, generate);
		verify(changes, never()).save(any());
	}

	@Test
	void shouldBlockSingleReschedulingOfPaidWork() {
		when(payments.findActiveByWork(organizationId, work.id())).thenReturn(Optional.of(payment()));
		assertThrows(PaymentConflictException.class,
				() -> reschedule.execute(context, new RescheduleWorkOrderInput(work.id(), today.plusDays(1), null)));
		assertEquals(work, storedWorks.get(work.id()));
	}

	@Test
	void shouldLockFamilyThenCheckReplayThenLockWorksBeforeFinancialReads() {
		var family = selectedFamily(ChangeScope.THIS_AND_FOLLOWING, anchor, List.of(new SelectedWork(work, 0)));
		cancelRecurring.execute(context, new CancelOccurrenceInput(family.selection(), List.of()));
		var order = inOrder(selection, changes, settlements, payments);
		order.verify(selection).lockFamily(context, family.selection());
		order.verify(changes).find(organizationId, anchor.getId(), "key");
		order.verify(selection).lockOccurrences(family);
		order.verify(settlements).hasRecordedForWork(organizationId, work.id());
		order.verify(payments).findActiveByWork(organizationId, work.id());
		order.verify(changes).save(any());
	}

	@Test
	void shouldReturnReschedulingReplayWithoutFinancialReadsOrMutations() {
		var family = selectedFamily(ChangeScope.THIS_AND_FOLLOWING, anchor, List.of());
		var input = new RescheduleOccurrenceInput(family.selection(), today.plusDays(1), null);
		when(changes.find(organizationId, anchor.getId(), "key"))
			.thenReturn(Optional.of(new RecurrenceChanges.Command(UUID.randomUUID(), organizationId, anchor.getId(),
					"key", RecurrenceCommandContent.reschedulingContent(input), actorId, clock.instant(), null)));
		rescheduleRecurring.execute(context, input);
		verify(selection, never()).lockOccurrences(any());
		verifyNoInteractions(payments, settlements, operations, series, generate);
		verify(changes, never()).save(any());
		verify(changes, never()).record(any());
	}

	private SelectionContext selectedFamily(ChangeScope scope, RecurrenceSeries version, List<SelectedWork> works) {
		var input = new OccurrenceSelection(work.id(), actorId, scope, "key");
		var family = new SelectionContext(input, organizationId, version, 0, List.of(version));
		when(selection.lockFamily(context, input)).thenReturn(family);
		when(selection.lockOccurrences(family)).thenReturn(works);
		return family;
	}

	private WorkOrder persistedWork() {
		var created = com.klaus.moply.factory.WorkOrderFactory.create(UUID.randomUUID(), UUID.randomUUID());
		return WorkOrder.restore(UUID.randomUUID(), created.customerId(), created.customerLocationId(),
				new WorkOrderSchedule(today, null), created.workDescription(), created.pricing(), created.status(), 0,
				created.workAssignments(), null);
	}

	private Payment payment() {
		return Payment.create(organizationId, new PaymentAmount(BigDecimal.TEN, "GBP"), today, clock.instant(),
				actorId);
	}

}
