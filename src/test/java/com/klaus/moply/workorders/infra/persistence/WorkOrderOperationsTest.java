package com.klaus.moply.workorders.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.factory.WorkOrderFactory;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkOrderOperationsTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID workId = UUID.randomUUID();

	private final LocalDate today = LocalDate.of(2026, 9, 28);

	private final WorkOrderJpaRepository repository = mock(WorkOrderJpaRepository.class);

	private final WorkOrderJpaRepositoryAdapter adapter = new WorkOrderJpaRepositoryAdapter(repository,
			mock(CustomerRepository.class), mock(CollaboratorRepository.class));

	private WorkOrderEntity stored;

	@BeforeEach
	void setUp() {
		stored = WorkOrderEntity.from(organizationId,
				WorkOrderFactory.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
		stored.setId(workId);
		stored.setVersion(4L);
		stored.setRecurrenceSeriesId(UUID.randomUUID());
		stored.setOccurrenceDate(today);
		when(repository.findForUpdate(organizationId, workId)).thenReturn(Optional.of(stored));
	}

	@ParameterizedTest
	@MethodSource("conditionChanges")
	void shouldRejectChangesToEveryImmutableCondition(Consumer<WorkOrderEntity> change) {
		var before = stored.toDomain();
		var candidate = WorkOrderEntity.from(organizationId, before);
		candidate.setId(workId);
		candidate.setVersion(before.version());
		change.accept(candidate);

		assertThrows(IllegalArgumentException.class,
				() -> adapter.update(organizationId, workId, work -> candidate.toDomain()));

		assertTrue(before.hasSameConditionsAs(stored.toDomain()));
		assertTrue(before.hasSameOperationAs(stored.toDomain()));
		verify(repository, never()).saveAndFlush(any());
	}

	static Stream<Consumer<WorkOrderEntity>> conditionChanges() {
		return Stream.of(e -> e.setId(UUID.randomUUID()), e -> e.setCustomerId(UUID.randomUUID()),
				e -> e.setCustomerLocationId(UUID.randomUUID()), e -> e.setDescription("Changed"),
				e -> e.setContractedHours(BigDecimal.ONE), e -> e.setHourlyRate(BigDecimal.ONE),
				e -> e.setAllocationPolicyVersion(7), e -> e.setVersion(5L),
				e -> e.getAssignments().getFirst().setCollaboratorId(UUID.randomUUID()),
				e -> e.setRecurrenceSeriesId(UUID.randomUUID()),
				e -> e.setOccurrenceDate(e.getOccurrenceDate().plusDays(1)), e -> {
					e.setTotalAmount(e.getTotalAmount().add(BigDecimal.ONE));
					var assignment = e.getAssignments().getFirst();
					assignment.setAllocatedAmount(assignment.getAllocatedAmount().add(BigDecimal.ONE));
				}, e -> {
					var first = e.getAssignments().getFirst();
					var last = e.getAssignments().getLast();
					first.setAllocatedAmount(first.getAllocatedAmount().add(BigDecimal.ONE));
					last.setAllocatedAmount(last.getAllocatedAmount().subtract(BigDecimal.ONE));
				});
	}

	@ParameterizedTest
	@MethodSource("operationalChanges")
	void shouldPersistOnlyOperationalFields(UnaryOperator<WorkOrder> transition) {
		var before = stored.toDomain();
		var expected = transition.apply(before);
		var assignments = stored.getAssignments();

		adapter.update(organizationId, workId, transition);

		assertTrue(expected.hasSameConditionsAs(stored.toDomain()));
		assertTrue(expected.hasSameOperationAs(stored.toDomain()));
		assertSame(assignments, stored.getAssignments());
		verify(repository).saveAndFlush(stored);
	}

	static Stream<UnaryOperator<WorkOrder>> operationalChanges() {
		return Stream.of(WorkOrder::complete, WorkOrder::cancel,
				work -> work.reschedule(LocalDate.of(2026, 10, 5), LocalTime.NOON, LocalDate.of(2026, 9, 28)));
	}

	@Test
	void shouldNotSaveAnEquivalentRestoredInstance() {
		adapter.update(organizationId, workId, work -> stored.toDomain());
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void shouldNotSaveRepeatedCompletionCancellationOrUnchangedSchedule() {
		stored.setStatus(WorkOrderStatus.COMPLETED);
		adapter.update(organizationId, workId, WorkOrder::complete);
		stored.setStatus(WorkOrderStatus.CANCELLED);
		adapter.update(organizationId, workId, WorkOrder::cancel);
		stored.setStatus(WorkOrderStatus.SCHEDULED);
		adapter.update(organizationId, workId, work -> work.reschedule(work.serviceDate(), work.startTime(), today));
		verify(repository, never()).saveAndFlush(any());
	}

}
