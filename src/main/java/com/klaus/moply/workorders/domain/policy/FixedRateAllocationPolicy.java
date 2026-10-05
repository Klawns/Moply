package com.klaus.moply.workorders.domain.policy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.entity.WorkAssignment;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

/** Fixed rates determine bases; the remaining price is shared equally in cents. */
public final class FixedRateAllocationPolicy {

	public Result calculate(DurationHours hours, HourlyRate rate, List<UUID> participants,
			List<BigDecimal> fixedRates) {
		var legacy = new ExactAllocationPolicy().calculate(hours, rate, participants);
		if (fixedRates == null || fixedRates.size() != participants.size())
			throw new DomainException("Tarifas devem corresponder aos participantes.");
		var effective = fixedRates.stream().map(f -> f == null ? rate : new HourlyRate(f)).toList();
		boolean different = effective.stream().anyMatch(f -> !f.equals(rate));
		var bases = new ArrayList<BigDecimal>();
		for (int i = 0; i < participants.size(); i++) {
			bases.add(different
					? hours.value()
						.multiply(effective.get(i).value())
						.divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.DOWN)
					: legacy.allocations().get(i).amount().value());
		}
		var baseTotal = bases.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		var remainder = legacy.total().value().subtract(baseTotal);
		var excess = remainder.signum() < 0 ? remainder.negate() : new BigDecimal("0.00");
		var surplus = remainder.signum() < 0 ? new BigDecimal("0.00") : remainder;
		BigInteger[] cents = surplus.movePointRight(2)
			.toBigIntegerExact()
			.divideAndRemainder(BigInteger.valueOf(participants.size()));
		var assignments = new ArrayList<WorkAssignment>();
		for (int i = 0; i < participants.size(); i++) {
			var extra = new BigDecimal(
					cents[0].add(BigInteger.valueOf(i).compareTo(cents[1]) < 0 ? BigInteger.ONE : BigInteger.ZERO), 2);
			assignments.add(new WorkAssignment(participants.get(i), i, new Money(bases.get(i).add(extra)),
					effective.get(i), fixedRates.get(i) != null, new Money(bases.get(i)), new Money(extra)));
		}
		return new Result(legacy.total(), different ? 2 : 1, different, excess.signum() == 0, baseTotal, surplus,
				excess, assignments);
	}

	public record Result(Money total, int policyVersion, boolean requiresConfirmation, boolean canCreate,
			BigDecimal baseTotal, BigDecimal surplusAmount, BigDecimal excessAmount, List<WorkAssignment> assignments) {
		public Result {
			assignments = List.copyOf(assignments);
		}
	}

}
