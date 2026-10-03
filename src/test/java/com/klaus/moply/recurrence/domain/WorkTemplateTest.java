package com.klaus.moply.recurrence.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.domain.vo.RecurrenceParticipants;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

import static org.junit.jupiter.api.Assertions.*;

class WorkTemplateTest {

	@Test
	void shouldKeepValidatedConditionsAndParticipantOrder() {
		var participants = new RecurrenceParticipants(List.of(UUID.randomUUID(), UUID.randomUUID()));
		var template = new WorkTemplate(UUID.randomUUID(), null, null, null, new DurationHours(BigDecimal.ONE),
				new HourlyRate(BigDecimal.TEN), "GBP", participants, WorkOrderStatus.COMPLETED);
		assertEquals(participants, template.participants());
		assertEquals(new DurationHours(BigDecimal.ONE), template.contractedHours());
	}

	@Test
	void shouldRejectInvalidConditionsWithExistingMessage() {
		var customer = UUID.randomUUID();
		var hours = new DurationHours(BigDecimal.ONE);
		var rate = new HourlyRate(BigDecimal.TEN);
		var participants = new RecurrenceParticipants(List.of(UUID.randomUUID()));
		List<org.junit.jupiter.api.function.Executable> invalid = List.of(
				() -> new WorkTemplate(null, null, null, null, hours, rate, "GBP", participants,
						WorkOrderStatus.SCHEDULED),
				() -> new WorkTemplate(customer, null, null, null, null, rate, "GBP", participants,
						WorkOrderStatus.SCHEDULED),
				() -> new WorkTemplate(customer, null, null, null, hours, null, "GBP", participants,
						WorkOrderStatus.SCHEDULED),
				() -> new WorkTemplate(customer, null, null, null, hours, rate, "USD", participants,
						WorkOrderStatus.SCHEDULED),
				() -> new WorkTemplate(customer, null, null, null, hours, rate, "GBP", null, WorkOrderStatus.SCHEDULED),
				() -> new WorkTemplate(customer, null, null, null, hours, rate, "GBP", participants, null),
				() -> new WorkTemplate(customer, null, null, null, hours, rate, "GBP", participants,
						WorkOrderStatus.CANCELLED));
		for (var construction : invalid) {
			assertEquals("Condições da série inválidas.",
					assertThrows(DomainException.class, construction).getMessage());
		}
	}

}
