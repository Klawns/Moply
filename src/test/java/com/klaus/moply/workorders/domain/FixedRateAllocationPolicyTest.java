package com.klaus.moply.workorders.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.klaus.moply.workorders.domain.policy.FixedRateAllocationPolicy;
import com.klaus.moply.workorders.domain.vo.*;

class FixedRateAllocationPolicyTest {

	final List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());

	FixedRateAllocationPolicy.Result calculate(String hours, String rate, BigDecimal... fixed) {
		return new FixedRateAllocationPolicy().calculate(new DurationHours(new BigDecimal(hours)),
				new HourlyRate(new BigDecimal(rate)), ids, Arrays.asList(fixed));
	}

	@Test
	void shouldDivideSurplusEquallyForMixedRates() {
		var result = calculate("4", "30", new BigDecimal("20"), null);
		assertEquals(new BigDecimal("120.00"), result.total().value());
		assertEquals(new BigDecimal("40.00"), result.assignments().getFirst().baseAmount().value());
		assertEquals(new BigDecimal("10.00"), result.assignments().getFirst().surplusAmount().value());
		assertEquals(new BigDecimal("50.00"), result.assignments().getFirst().allocatedAmount().value());
		assertEquals(new BigDecimal("70.00"), result.assignments().getLast().allocatedAmount().value());
		assertTrue(result.requiresConfirmation());
		assertTrue(result.canCreate());
		assertEquals(2, result.policyVersion());
	}

	@Test
	void shouldBlockWhenBasesExceedPrice() {
		var result = calculate("4", "30", new BigDecimal("40"), null);
		assertFalse(result.canCreate());
		assertEquals(new BigDecimal("20.00"), result.excessAmount());
	}

	@Test
	void shouldPreserveLegacyCentDistributionWithoutDifferentRates() {
		for (var result : List.of(calculate("0.01", "1", null, null), calculate("0.01", "1", BigDecimal.ONE, null))) {
			assertFalse(result.requiresConfirmation());
			assertEquals(1, result.policyVersion());
			assertEquals(new BigDecimal("0.01"), result.assignments().getFirst().allocatedAmount().value());
			assertEquals(new BigDecimal("0.00"), result.assignments().getLast().allocatedAmount().value());
		}
	}

	@Test
	void shouldHandleAllFixedRatesAndDistributeRemainderByPosition() {
		var result = calculate("0.03", "1", new BigDecimal("0.10"), new BigDecimal("0.20"));
		assertEquals(new BigDecimal("0.02"), result.assignments().getFirst().allocatedAmount().value());
		assertEquals(new BigDecimal("0.01"), result.assignments().getLast().allocatedAmount().value());
	}

	@Test
	void shouldNotRoundIndividualHoursBeforeMultiplication() {
		var three = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		var result = new FixedRateAllocationPolicy().calculate(new DurationHours(BigDecimal.ONE),
				new HourlyRate(new BigDecimal("30")), three, Arrays.asList(new BigDecimal("20"), null, null));
		assertEquals(new BigDecimal("6.66"), result.assignments().getFirst().baseAmount().value());
		assertEquals(new BigDecimal("30.00"),
				result.assignments()
					.stream()
					.map(a -> a.allocatedAmount().value())
					.reduce(BigDecimal.ZERO, BigDecimal::add));
	}

}
