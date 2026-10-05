package com.klaus.moply.workorders.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.entity.*;
import com.klaus.moply.workorders.domain.vo.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkOrderTest {

	private final UUID customer = UUID.randomUUID();

	private final UUID first = UUID.randomUUID();

	private final UUID second = UUID.randomUUID();

	private WorkOrder create(List<UUID> ids, WorkOrderStatus status) {
		return WorkOrder.create(customer, null, new WorkOrderSchedule(LocalDate.of(2026, 9, 28), null),
				new WorkOrderDescription("  "), new DurationHours(new BigDecimal("0.01")),
				new HourlyRate(BigDecimal.ONE), ids, status);
	}

	@Test
	void shouldKeepOptionalFieldsAndZeroAllocationsAndDeriveCount() {
		var ids = new ArrayList<>(List.of(first, second));
		var work = create(ids, WorkOrderStatus.COMPLETED);
		ids.clear();
		assertNull(work.customerLocationId());
		assertNull(work.startTime());
		assertNull(work.description());
		assertEquals(2, work.participantCount());
		assertEquals(WorkOrderStatus.COMPLETED, work.status());
		assertEquals(new BigDecimal("0.01"), work.assignments().getFirst().allocatedAmount().value());
		assertEquals(new BigDecimal("0.00"), work.assignments().getLast().allocatedAmount().value());
		assertThrows(UnsupportedOperationException.class, () -> work.assignments().clear());
	}

	@Test
	void shouldRejectMissingDuplicateAndNullParticipantsAndInvalidInitialStatus() {
		for (var ids : List.of(List.<UUID>of(), List.of(first, first), Arrays.asList(first, null)))
			assertThrows(DomainException.class, () -> create(ids, WorkOrderStatus.SCHEDULED));
		assertThrows(DomainException.class, () -> create(null, WorkOrderStatus.SCHEDULED));
		assertThrows(DomainException.class, () -> create(List.of(first), WorkOrderStatus.CANCELLED));
		assertThrows(DomainException.class, () -> create(List.of(first), null));
	}

	private WorkOrder restore(List<WorkAssignment> assignments) {
		return WorkOrder.restore(UUID.randomUUID(), customer, null, new WorkOrderSchedule(LocalDate.now(), null),
				new WorkOrderDescription(null),
				new WorkOrderPricing(new DurationHours(BigDecimal.ONE), new HourlyRate(BigDecimal.TEN), "GBP",
						new Money(BigDecimal.ONE), 7),
				WorkOrderStatus.CANCELLED, 4, new WorkOrderAssignments(assignments), null);
	}

	@Test
	void shouldRestoreHistoricalAmountsAndPolicyWithoutRecalculating() {
		var restored = restore(List.of(new WorkAssignment(first, 0, new Money(new BigDecimal("0.20"))),
				new WorkAssignment(second, 1, new Money(new BigDecimal("0.80")))));
		assertEquals(7, restored.allocationPolicyVersion());
		assertEquals(4, restored.version());
		assertEquals(new BigDecimal("1.00"), restored.totalAmount().value());
		assertEquals(new BigDecimal("0.80"), restored.assignments().getLast().allocatedAmount().value());
	}

	@Test
	void shouldRejectCorruptStoredAssignmentsWithoutRepairingThem() {
		assertThrows(DomainException.class,
				() -> restore(List.of(new WorkAssignment(first, 1, new Money(BigDecimal.ONE)))));
		assertThrows(DomainException.class,
				() -> restore(List.of(new WorkAssignment(first, 0, new Money(BigDecimal.ZERO)))));
		assertThrows(DomainException.class,
				() -> restore(List.of(new WorkAssignment(first, 0, new Money(BigDecimal.ONE)),
						new WorkAssignment(first, 1, new Money(BigDecimal.ZERO)))));
		assertThrows(DomainException.class, () -> restore(List.of()));
	}

	@Test
	void shouldValidateRequiredAggregateDataAndRestorationIdentity() {
		var work = create(List.of(first), WorkOrderStatus.SCHEDULED);
		var id = UUID.randomUUID();
		assertThrows(DomainException.class, () -> WorkOrder.restore(null, customer, null, work.schedule(),
				work.workDescription(), work.pricing(), work.status(), 0, work.workAssignments(), null));
		assertThrows(DomainException.class, () -> WorkOrder.restore(id, customer, null, work.schedule(),
				work.workDescription(), work.pricing(), work.status(), -1, work.workAssignments(), null));
		assertThrows(DomainException.class, () -> WorkOrder.create(null, null, work.schedule(), work.workDescription(),
				work.contractedHours(), work.hourlyRate(), List.of(first), work.status()));
		assertThrows(DomainException.class, () -> WorkOrder.restore(id, customer, null, null, work.workDescription(),
				work.pricing(), work.status(), 0, work.workAssignments(), null));
		assertThrows(DomainException.class, () -> WorkOrder.restore(id, customer, null, work.schedule(), null,
				work.pricing(), work.status(), 0, work.workAssignments(), null));
		assertThrows(DomainException.class, () -> WorkOrder.restore(id, customer, null, work.schedule(),
				work.workDescription(), null, work.status(), 0, work.workAssignments(), null));
		assertThrows(DomainException.class, () -> WorkOrder.restore(id, customer, null, work.schedule(),
				work.workDescription(), work.pricing(), null, 0, work.workAssignments(), null));
		assertThrows(DomainException.class, () -> WorkOrder.restore(id, customer, null, work.schedule(),
				work.workDescription(), work.pricing(), work.status(), 0, null, null));
	}

	@Test
	void shouldKeepOriginalStateAndOccurrenceAcrossOperationalChanges() {
		var created = create(List.of(first), WorkOrderStatus.SCHEDULED);
		var occurrence = new OccurrenceIdentity(UUID.randomUUID(), LocalDate.of(2026, 9, 28));
		var work = WorkOrder.restore(UUID.randomUUID(), customer, null, created.schedule(), created.workDescription(),
				created.pricing(), created.status(), 4, created.workAssignments(), occurrence);
		var changed = work.reschedule(LocalDate.of(2026, 10, 5), null, LocalDate.of(2026, 9, 28)).complete().cancel();
		assertEquals(WorkOrderStatus.SCHEDULED, work.status());
		assertEquals(LocalDate.of(2026, 9, 28), work.serviceDate());
		assertEquals(occurrence, changed.occurrence());
		assertTrue(work.hasSameConditionsAs(changed));
		assertFalse(work.hasSameOperationAs(changed));
		assertFalse(work.hasSameConditionsAs(null));
		assertFalse(work.hasSameOperationAs(null));
	}

}
