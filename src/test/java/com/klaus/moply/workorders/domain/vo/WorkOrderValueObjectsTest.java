package com.klaus.moply.workorders.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.entity.WorkAssignment;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderValueObjectsTest {

	@Test
	void shouldRequireServiceDateAndKeepOptionalTime() {
		assertThrows(DomainException.class, () -> new WorkOrderSchedule(null, LocalTime.NOON));
		var date = LocalDate.of(2026, 10, 5);
		assertNull(new WorkOrderSchedule(date, null).startTime());
		assertEquals(LocalTime.NOON, new WorkOrderSchedule(date, LocalTime.NOON).startTime());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "  ", "\t\n", "\u2003" })
	void shouldNormalizeMissingDescription(String value) {
		assertNull(new WorkOrderDescription(value).value());
	}

	@Test
	void shouldStripDescriptionWithoutChangingItsContent() {
		assertEquals("Visit  customer", new WorkOrderDescription(" \tVisit  customer\n ").value());
	}

	@ParameterizedTest
	@MethodSource("invalidPricing")
	void shouldRejectInvalidCommercialConditions(DurationHours hours, HourlyRate rate, String currency, Money total,
			int policyVersion) {
		assertThrows(DomainException.class, () -> new WorkOrderPricing(hours, rate, currency, total, policyVersion));
	}

	static Stream<Arguments> invalidPricing() {
		var hours = new DurationHours(BigDecimal.ONE);
		var rate = new HourlyRate(BigDecimal.TEN);
		var total = new Money(BigDecimal.ONE);
		return Stream.of(Arguments.of(null, rate, "GBP", total, 1), Arguments.of(hours, null, "GBP", total, 1),
				Arguments.of(hours, rate, null, total, 1), Arguments.of(hours, rate, "EUR", total, 1),
				Arguments.of(hours, rate, "GBP", null, 1),
				Arguments.of(hours, rate, "GBP", new Money(BigDecimal.ZERO), 1),
				Arguments.of(hours, rate, "GBP", total, 0), Arguments.of(hours, rate, "GBP", total, -1));
	}

	@Test
	void shouldPreserveHistoricalPricingWithoutRecalculating() {
		var pricing = new WorkOrderPricing(new DurationHours(BigDecimal.ONE), new HourlyRate(BigDecimal.TEN), "GBP",
				new Money(BigDecimal.ONE), 7);
		assertEquals(new BigDecimal("1.00"), pricing.totalAmount().value());
		assertEquals(7, pricing.allocationPolicyVersion());
	}

	@Test
	void shouldCopyAssignmentsAndAllowZeroShares() {
		var values = new ArrayList<>(List.of(new WorkAssignment(UUID.randomUUID(), 0, new Money(BigDecimal.ONE)),
				new WorkAssignment(UUID.randomUUID(), 1, new Money(BigDecimal.ZERO))));
		var assignments = new WorkOrderAssignments(values);
		values.clear();
		assertEquals(2, assignments.count());
		assertEquals(new Money(BigDecimal.ONE), assignments.totalAmount());
		assertThrows(UnsupportedOperationException.class, () -> assignments.values().clear());
	}

	@Test
	void shouldRejectMissingNullDuplicateAndUnorderedAssignments() {
		var id = UUID.randomUUID();
		var first = new WorkAssignment(id, 0, new Money(BigDecimal.ONE));
		assertThrows(DomainException.class, () -> new WorkOrderAssignments(null));
		assertThrows(DomainException.class, () -> new WorkOrderAssignments(List.of()));
		assertThrows(DomainException.class, () -> new WorkOrderAssignments(Arrays.asList(first, null)));
		assertThrows(DomainException.class,
				() -> new WorkOrderAssignments(List.of(first, new WorkAssignment(id, 1, new Money(BigDecimal.ZERO)))));
		assertThrows(DomainException.class,
				() -> new WorkOrderAssignments(List.of(new WorkAssignment(id, 1, new Money(BigDecimal.ONE)))));
		assertThrows(DomainException.class, () -> new WorkOrderAssignments(
				List.of(first, new WorkAssignment(UUID.randomUUID(), 2, new Money(BigDecimal.ZERO)))));
	}

}
