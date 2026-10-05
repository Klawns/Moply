package com.klaus.moply.workorders.application.usecase.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PricingPreviewOutput(String pricingFingerprint, String currencyCode, BigDecimal contractedHours,
		BigDecimal hourlyRate, BigDecimal totalAmount, int allocationPolicyVersion, boolean requiresConfirmation,
		boolean canCreate, BigDecimal baseTotal, BigDecimal surplusAmount, BigDecimal excessAmount,
		int participantCount, List<Participant> participants) {

	public PricingPreviewOutput {
		participants = List.copyOf(participants);
	}
	public record Participant(UUID collaboratorId, int inclusionPosition, BigDecimal appliedHourlyRate,
			String rateSource, BigDecimal individualHours, BigDecimal baseAmount, BigDecimal surplusAmount,
			BigDecimal allocatedAmount) {
	}
}
