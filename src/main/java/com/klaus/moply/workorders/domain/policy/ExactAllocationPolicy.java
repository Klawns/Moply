package com.klaus.moply.workorders.domain.policy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

/** Splits the rounded total in cents, assigning remainder cents in participant order. */
public final class ExactAllocationPolicy {

	private static final int POLICY_VERSION = 1;

	public Result calculate(DurationHours hours, HourlyRate hourlyRate, List<UUID> participantIds) {
		var participants = validateInputs(hours, hourlyRate, participantIds);
		Money total = calculateTotal(hours, hourlyRate);
		return new Result(hours, hourlyRate, total, POLICY_VERSION, distribute(total, participants));
	}

	private List<UUID> validateInputs(DurationHours hours, HourlyRate hourlyRate, List<UUID> participantIds) {
		if (hours == null || hourlyRate == null || participantIds == null || participantIds.isEmpty()) {
			throw new DomainException("Horas, tarifa e participantes são obrigatórios.");
		}
		var participants = new ArrayList<>(participantIds);
		var uniqueIds = new HashSet<UUID>();
		for (UUID id : participants) {
			if (id == null || !uniqueIds.add(id)) {
				throw new DomainException("Participantes devem ter IDs não nulos e únicos.");
			}
		}
		return participants;
	}

	private Money calculateTotal(DurationHours hours, HourlyRate hourlyRate) {
		var totalValue = hours.value().multiply(hourlyRate.value()).setScale(2, RoundingMode.HALF_EVEN);
		if (totalValue.signum() == 0) {
			throw new DomainException("O total arredondado deve ser maior que zero.");
		}
		return new Money(totalValue);
	}

	private List<Allocation> distribute(Money total, List<UUID> participants) {
		BigInteger totalInCents = total.value().movePointRight(2).toBigIntegerExact();
		BigInteger[] quotientAndRemainder = totalInCents.divideAndRemainder(BigInteger.valueOf(participants.size()));
		BigInteger quotient = quotientAndRemainder[0];
		BigInteger remainder = quotientAndRemainder[1];
		var allocations = new ArrayList<Allocation>(participants.size());
		for (int position = 0; position < participants.size(); position++) {
			BigInteger amount = quotient;
			if (BigInteger.valueOf(position).compareTo(remainder) < 0) {
				amount = amount.add(BigInteger.ONE);
			}
			allocations.add(new Allocation(participants.get(position), position, new Money(new BigDecimal(amount, 2))));
		}
		return List.copyOf(allocations);
	}

	public record Allocation(UUID participantId, int inclusionPosition, Money amount) {
	}

	public record Result(DurationHours hours, HourlyRate hourlyRate, Money total, int policyVersion,
			List<Allocation> allocations) {
		public Result {
			allocations = List.copyOf(allocations);
		}

		public BigDecimal approximateIndividualHours() {
			return hours.value().divide(BigDecimal.valueOf(allocations.size()), 2, RoundingMode.HALF_EVEN);
		}
	}

}
