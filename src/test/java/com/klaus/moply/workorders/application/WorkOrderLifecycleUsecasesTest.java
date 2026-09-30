package com.klaus.moply.workorders.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.application.usecase.*;
import com.klaus.moply.workorders.domain.entity.*;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;

class WorkOrderLifecycleUsecasesTest {

	@Test
	void shouldUseAccountDateAtMidnightAndDaylightSavingBoundaries() {
		assertReschedule("2026-09-30T23:30:00Z", "Europe/London", LocalDate.of(2026, 10, 1), false);
		assertReschedule("2026-09-30T23:30:00Z", "America/Los_Angeles", LocalDate.of(2026, 10, 1), true);
		assertReschedule("2026-10-25T00:30:00Z", "Europe/London", LocalDate.of(2026, 10, 25), false);
		assertReschedule("2026-10-25T01:30:00Z", "Europe/London", LocalDate.of(2026, 10, 25), false);
		assertReschedule("2026-03-29T00:30:00Z", "Europe/London", LocalDate.of(2026, 3, 30), true);
		assertReschedule("2026-03-29T01:30:00Z", "Europe/London", LocalDate.of(2026, 3, 30), true);
	}

	private void assertReschedule(String instant, String zone, LocalDate date, boolean allowed) {
		var account = UUID.randomUUID();
		var id = UUID.randomUUID();
		var accounts = mock(OrganizationRepository.class);
		var operations = mock(WorkOrderOperations.class);
		when(accounts.findById(account))
			.thenReturn(Optional.of(new Organization(account, "Account", zone, DefaultWorkStatus.COMPLETED)));
		var original = WorkOrder.create(UUID.randomUUID(), null, date, null, null, BigDecimal.ONE, BigDecimal.TEN,
				List.of(UUID.randomUUID()), WorkOrderStatus.COMPLETED);
		var current = new AtomicReference<>(original);
		doAnswer(invocation -> {
			UnaryOperator<WorkOrder> transition = invocation.getArgument(2);
			current.set(transition.apply(current.get()));
			return null;
		}).when(operations).update(eq(account), eq(id), any());
		// Deliberately different from both account and server zones.
		var usecase = new RescheduleWorkOrder(operations, accounts,
				Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Tokyo")));
		var input = new RescheduleWorkOrder.Input(id, date.plusDays(7), LocalTime.NOON);
		if (allowed) {
			usecase.execute(new Context(account), input);
			assertEquals(date.plusDays(7), current.get().serviceDate());
			assertEquals(WorkOrderStatus.COMPLETED, current.get().status());
		}
		else {
			assertThrows(WorkOrderStateException.class, () -> usecase.execute(new Context(account), input));
			assertSame(original, current.get());
		}
		verify(operations).update(eq(account), eq(id), any());
	}

}
