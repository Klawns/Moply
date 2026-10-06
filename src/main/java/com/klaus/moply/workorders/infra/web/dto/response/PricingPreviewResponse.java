package com.klaus.moply.workorders.infra.web.dto.response;

import java.util.List;

import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;

public record PricingPreviewResponse(String pricingFingerprint, boolean requiresConfirmation, boolean canCreate,
		int participantCount, WorkPricingResponse pricing, PricingPreviewSummaryResponse summary,
		List<PricingPreviewParticipantResponse> participants) {

	public static PricingPreviewResponse from(PricingPreviewOutput p) {
		return new PricingPreviewResponse(p.pricingFingerprint(), p.requiresConfirmation(), p.canCreate(),
				p.participantCount(),
				new WorkPricingResponse(p.contractedHours(), p.hourlyRate(), p.currencyCode(), p.totalAmount(),
						p.allocationPolicyVersion()),
				new PricingPreviewSummaryResponse(p.baseTotal(), p.surplusAmount(), p.excessAmount()),
				p.participants().stream().map(PricingPreviewParticipantResponse::from).toList());
	}

}
