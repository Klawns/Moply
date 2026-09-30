package com.klaus.moply.workorders.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

class ExactAllocationPolicyTest {

	private final ExactAllocationPolicy policy = new ExactAllocationPolicy();

	private List<UUID> participants(int count) {
		return IntStream.range(0, count).mapToObj(i -> new UUID(0, i + 1)).toList();
	}

	private ExactAllocationPolicy.Result calculate(String hours, String rate, int count) {
		return policy.calculate(new DurationHours(new BigDecimal(hours)), new HourlyRate(new BigDecimal(rate)),
				participants(count));
	}

	@ParameterizedTest
	@CsvSource({ "3,11.50,4,34.50,8.63;8.63;8.62;8.62", "1,10,3,10.00,3.34;3.33;3.33",
			"0.01,2,4,0.02,0.01;0.01;0.00;0.00" })
	void shouldAllocateExactExamples(String hours, String rate, int count, String total, String amounts) {
		var result = calculate(hours, rate, count);
		assertEquals(new BigDecimal(total), result.total().value());
		assertEquals(Arrays.stream(amounts.split(";")).map(BigDecimal::new).toList(),
				result.allocations().stream().map(a -> a.amount().value()).toList());
		assertEquals(new DurationHours(new BigDecimal(hours)), result.hours());
		assertEquals(new HourlyRate(new BigDecimal(rate)), result.hourlyRate());
		assertEquals(1, result.policyVersion());
		for (int i = 0; i < count; i++) {
			assertEquals(participants(count).get(i), result.allocations().get(i).participantId());
			assertEquals(i, result.allocations().get(i).inclusionPosition());
		}
	}

	@Test
	void shouldConserveTotalAcrossTeamSizesAndAmounts() {
		for (String total : List.of("0.01", "0.02", "1.00", "10.00", "34.50", "999.99", "92233720368547758.09")) {
			for (int count : List.of(1, 2, 3, 4, 7, 31, 100)) {
				var result = calculate("1", total, count);
				assertEquals(result, calculate("1", total, count));
				assertEquals(result.total().value(),
						result.allocations()
							.stream()
							.map(a -> a.amount().value())
							.reduce(new BigDecimal("0.00"), BigDecimal::add));
				var values = result.allocations().stream().map(a -> a.amount().value()).toList();
				assertTrue(Collections.max(values)
					.subtract(Collections.min(values))
					.compareTo(new BigDecimal("0.01")) <= 0);
				assertTrue(values.stream().allMatch(v -> v.signum() >= 0 && v.scale() == 2));
			}
		}
	}

	@Test
	void shouldHandleCentsAboveLongMaximum() {
		var result = calculate("1", "92233720368547758.09", 2);
		assertTrue(result.total()
			.value()
			.movePointRight(2)
			.toBigIntegerExact()
			.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
		assertEquals(List.of(new BigDecimal("46116860184273879.05"), new BigDecimal("46116860184273879.04")),
				result.allocations().stream().map(a -> a.amount().value()).toList());
	}

	@Test
	void shouldAssignRemainderByInputOrder() {
		var ids = new ArrayList<>(participants(3));
		var first = policy.calculate(new DurationHours(BigDecimal.ONE), new HourlyRate(BigDecimal.TEN), ids);
		Collections.reverse(ids);
		var reversed = policy.calculate(first.hours(), first.hourlyRate(), ids);
		assertEquals(first.allocations().getFirst().amount(), reversed.allocations().getFirst().amount());
		assertEquals(ids.getFirst(), reversed.allocations().getFirst().participantId());
		assertEquals(first.allocations().getFirst().participantId(), reversed.allocations().getLast().participantId());
		assertEquals(new BigDecimal("3.33"), reversed.allocations().getLast().amount().value());
	}

	@ParameterizedTest
	@CsvSource({ "0.5,0.05,0.02", "0.5,0.07,0.04" })
	void shouldRoundTotalHalfEven(String hours, String rate, String total) {
		assertEquals(new BigDecimal(total), calculate(hours, rate, 1).total().value());
	}

	@Test
	void shouldRejectRoundedZeroTotal() {
		assertThrows(DomainException.class, () -> calculate("0.01", "0.01", 1));
		assertThrows(DomainException.class, () -> calculate("0.1", "0.05", 1));
	}

	@Test
	void shouldRejectInvalidArguments() {
		var hours = new DurationHours(BigDecimal.ONE);
		var rate = new HourlyRate(BigDecimal.ONE);
		assertThrows(DomainException.class, () -> policy.calculate(null, rate, participants(1)));
		assertThrows(DomainException.class, () -> policy.calculate(hours, null, participants(1)));
		assertThrows(DomainException.class, () -> policy.calculate(hours, rate, null));
		assertThrows(DomainException.class, () -> policy.calculate(hours, rate, List.of()));
		assertThrows(DomainException.class,
				() -> policy.calculate(hours, rate, Arrays.asList(UUID.randomUUID(), null)));
		var id = UUID.randomUUID();
		assertThrows(DomainException.class, () -> policy.calculate(hours, rate, List.of(id, id)));
	}

	@Test
	void shouldDefensivelyCopyListsAndExposeImmutableResults() {
		var ids = new ArrayList<>(participants(3));
		var result = policy.calculate(new DurationHours(BigDecimal.ONE), new HourlyRate(BigDecimal.TEN), ids);
		ids.clear();
		assertEquals(3, result.allocations().size());
		assertThrows(UnsupportedOperationException.class, () -> result.allocations().clear());
		var allocations = new ArrayList<>(result.allocations());
		var copy = new ExactAllocationPolicy.Result(result.hours(), result.hourlyRate(), result.total(),
				result.policyVersion(), allocations);
		allocations.clear();
		assertEquals(result, copy);
	}

	@Test
	void shouldKeepApproximateHoursIndependentFromMoney() {
		var result = calculate("1", "10", 3);
		assertEquals(new BigDecimal("0.33"), result.approximateIndividualHours());
		assertEquals(new BigDecimal("3.34"), result.allocations().getFirst().amount().value());
		assertEquals(new BigDecimal("0.00"), calculate("0.01", "2", 4).approximateIndividualHours());
		assertEquals(new BigDecimal("0.02"), calculate("0.05", "1", 2).approximateIndividualHours());
		assertEquals(new BigDecimal("0.04"), calculate("0.07", "1", 2).approximateIndividualHours());
	}

}
