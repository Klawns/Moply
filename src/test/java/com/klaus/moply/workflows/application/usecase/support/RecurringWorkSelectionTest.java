package com.klaus.moply.workflows.application.usecase.support;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.klaus.moply.factory.WorkOrderFactory;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import com.klaus.moply.recurrence.domain.vo.SeriesVersion;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.OccurrenceSelection;
import com.klaus.moply.workflows.application.usecase.dto.SelectedWork;
import com.klaus.moply.workflows.application.usecase.dto.SelectionContext;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecurringWorkSelectionTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID seriesId = UUID.randomUUID();

	private final UUID workId = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private final LocalDate originalDate = LocalDate.of(2026, 10, 5);

	private final RecurrenceRepository series = mock(RecurrenceRepository.class);

	private final WorkOrderOccurrences occurrences = mock(WorkOrderOccurrences.class);

	private final WorkOrderOperations operations = mock(WorkOrderOperations.class);

	private final RecurrenceSeries anchor = mock(RecurrenceSeries.class);

	private final RecurringWorkSelection service = new RecurringWorkSelection(series, occurrences, operations);

	@Test
	void shouldRejectNonRecurringWorkBeforeLockingFamily() {
		when(occurrences.reference(organizationId, workId))
			.thenReturn(new WorkOrderOccurrences.Reference(workId, null, null));
		assertThrows(DomainException.class,
				() -> service.lockFamily(new Context(organizationId), input(ChangeScope.THIS_OCCURRENCE)));
		verifyNoInteractions(series, operations);
	}

	@Test
	void shouldLockOnlySelectedOccurrenceUsingOriginalPosition() {
		var family = lockFamily(ChangeScope.THIS_OCCURRENCE);
		when(anchor.getLineage()).thenReturn(new SeriesVersion(seriesId, null, 0, null));
		applyLockedWork();
		var selected = service.lockOccurrences(family);
		assertEquals(1, selected.size());
		assertEquals(2, selected.getFirst().position());
		verify(operations).withWorkOrder(eq(organizationId), eq(workId), any());
		verify(occurrences, never()).inSeries(any(), any());
	}

	@Test
	void shouldRejectReplacedOccurrenceBeforeLockingWork() {
		var family = lockFamily(ChangeScope.THIS_OCCURRENCE);
		when(anchor.getLineage()).thenReturn(new SeriesVersion(seriesId, null, 0, 2L));
		assertThrows(WorkOrderStateException.class, () -> service.lockOccurrences(family));
		verifyNoInteractions(operations);
	}

	@Test
	void shouldSelectFollowingVersionsAndLockIdsInDeterministicOrder() {
		var successor = mock(RecurrenceSeries.class);
		var successorId = UUID.randomUUID();
		var earlierId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		var excludedId = UUID.randomUUID();
		when(successor.getId()).thenReturn(successorId);
		when(successor.getLineage()).thenReturn(new SeriesVersion(seriesId, seriesId, 3, null));
		when(successor.positionOf(originalDate.plusDays(7))).thenReturn(3L);
		when(anchor.getId()).thenReturn(seriesId);
		when(anchor.getLineage()).thenReturn(new SeriesVersion(seriesId, null, 0, 3L));
		when(anchor.positionOf(originalDate)).thenReturn(2L);
		when(anchor.positionOf(originalDate.minusDays(7))).thenReturn(1L);
		when(occurrences.reference(organizationId, workId))
			.thenReturn(new WorkOrderOccurrences.Reference(workId, seriesId, originalDate));
		when(series.lockFamily(organizationId, seriesId)).thenReturn(List.of(anchor, successor));
		when(occurrences.inSeries(organizationId, seriesId))
			.thenReturn(List.of(new WorkOrderOccurrences.Reference(workId, seriesId, originalDate),
					new WorkOrderOccurrences.Reference(excludedId, seriesId, originalDate.minusDays(7))));
		when(occurrences.inSeries(organizationId, successorId))
			.thenReturn(List.of(new WorkOrderOccurrences.Reference(earlierId, successorId, originalDate.plusDays(7))));
		applyLockedWork();
		var family = service.lockFamily(new Context(organizationId), input(ChangeScope.THIS_AND_FOLLOWING));
		var selected = service.lockOccurrences(family);
		assertEquals(List.of(3L, 2L), selected.stream().map(SelectedWork::position).toList());
		var order = inOrder(operations);
		order.verify(operations).withWorkOrder(eq(organizationId), eq(earlierId), any());
		order.verify(operations).withWorkOrder(eq(organizationId), eq(workId), any());
		order.verifyNoMoreInteractions();
	}

	private SelectionContext lockFamily(ChangeScope scope) {
		when(anchor.getId()).thenReturn(seriesId);
		when(anchor.positionOf(originalDate)).thenReturn(2L);
		when(occurrences.reference(organizationId, workId))
			.thenReturn(new WorkOrderOccurrences.Reference(workId, seriesId, originalDate));
		when(series.lockFamily(organizationId, seriesId)).thenReturn(List.of(anchor));
		return service.lockFamily(new Context(organizationId), input(scope));
	}

	private OccurrenceSelection input(ChangeScope scope) {
		return new OccurrenceSelection(workId, UUID.randomUUID(), scope, "key");
	}

	@SuppressWarnings("unchecked")
	private void applyLockedWork() {
		when(operations.withWorkOrder(eq(organizationId), any(), any())).thenAnswer(invocation -> {
			Function<com.klaus.moply.workorders.domain.entity.WorkOrder, Object> operation = invocation.getArgument(2);
			return operation.apply(WorkOrderFactory.create(UUID.randomUUID(), UUID.randomUUID()));
		});
	}

}
