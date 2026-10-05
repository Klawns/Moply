package com.klaus.moply.workorders.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.domain.entity.*;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import com.klaus.moply.workorders.domain.vo.WorkOrderDescription;
import com.klaus.moply.workorders.domain.vo.WorkOrderSchedule;

class WorkOrderLifecycleTest {

	private final LocalDate today = LocalDate.of(2026, 9, 30);

	private WorkOrder work(LocalDate date, WorkOrderStatus state) {
		var work = WorkOrder.create(UUID.randomUUID(), UUID.randomUUID(), new WorkOrderSchedule(date, LocalTime.NOON),
				new WorkOrderDescription("Visit"), new DurationHours(new BigDecimal("3")),
				new HourlyRate(new BigDecimal("11.50")),
				List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
				WorkOrderStatus.SCHEDULED);
		return state == WorkOrderStatus.CANCELLED ? work.cancel()
				: state == WorkOrderStatus.COMPLETED ? work.complete() : work;
	}

	@Test
	void shouldCompleteAnyDateAndRepeatWithoutChanges() {
		for (var date : List.of(today.minusDays(1), today, today.plusDays(1))) {
			var scheduled = work(date, WorkOrderStatus.SCHEDULED);
			var completed = scheduled.complete();
			assertEquals(WorkOrderStatus.COMPLETED, completed.status());
			assertSame(completed, completed.complete());
			assertEquals(scheduled.assignments(), completed.assignments());
			assertEquals(scheduled.serviceDate(), completed.serviceDate());
		}
	}

	@Test
	void shouldCancelEveryStateAndRejectFurtherActiveOperations() {
		for (var state : WorkOrderStatus.values()) {
			var original = work(today.plusDays(1), state);
			var cancelled = original.cancel();
			assertSame(cancelled, cancelled.cancel());
			assertEquals(original.assignments(), cancelled.assignments());
			assertEquals(original.totalAmount(), cancelled.totalAmount());
			assertThrows(WorkOrderStateException.class, cancelled::complete);
			assertThrows(WorkOrderStateException.class, () -> cancelled.reschedule(today.plusDays(2), null, today));
		}
	}

	@Test
	void shouldRescheduleScheduledAndFutureCompletedWithoutChangingConditions() {
		for (var state : List.of(WorkOrderStatus.SCHEDULED, WorkOrderStatus.COMPLETED)) {
			var original = work(today.plusDays(1), state);
			var changed = original.reschedule(today.minusDays(2), null, today);
			assertEquals(today.minusDays(2), changed.serviceDate());
			assertNull(changed.startTime());
			assertEquals(state, changed.status());
			assertEquals(original.assignments(), changed.assignments());
			assertEquals(original.customerLocationId(), changed.customerLocationId());
			assertEquals(original.description(), changed.description());
			assertEquals(original.contractedHours(), changed.contractedHours());
			assertEquals(original.hourlyRate(), changed.hourlyRate());
			assertEquals(original.allocationPolicyVersion(), changed.allocationPolicyVersion());
		}
		assertEquals(today.plusDays(5),
				work(today.minusDays(2), WorkOrderStatus.SCHEDULED).reschedule(today.plusDays(5), LocalTime.NOON, today)
					.serviceDate());
	}

	@Test
	void shouldRejectCompletedDateReachedAndMissingTargetDate() {
		for (var date : List.of(today.minusDays(1), today)) {
			var completed = work(date, WorkOrderStatus.COMPLETED);
			assertThrows(WorkOrderStateException.class, () -> completed.reschedule(today.plusDays(3), null, today));
		}
		assertThrows(DomainException.class, () -> work(today, WorkOrderStatus.SCHEDULED).reschedule(null, null, today));
	}

}
