package com.klaus.moply.workorders.infra.web.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;

public record PricingPreviewParticipantResponse(UUID collaboratorId, int inclusionPosition,
		BigDecimal appliedHourlyRate, String rateSource, BigDecimal individualHours,
		BigDecimal baseAmount, BigDecimal surplusAmount, BigDecimal allocatedAmount) {

	public static PricingPreviewParticipantResponse from(PricingPreviewOutput.Participant p) {
		return new PricingPreviewParticipantResponse(p.collaboratorId(), p.inclusionPosition(), p.appliedHourlyRate(),
				p.rateSource(), p.individualHours(), p.baseAmount(), p.surplusAmount(), p.allocatedAmount());
	}

}
