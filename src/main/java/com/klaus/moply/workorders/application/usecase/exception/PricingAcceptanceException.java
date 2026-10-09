package com.klaus.moply.workorders.application.usecase.exception;

import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class PricingAcceptanceException extends ApplicationException {

	private final PricingPreviewOutput preview;

	public PricingAcceptanceException(PricingPreviewOutput preview) {
		super(preview.canCreate() ? "PRICING_ACCEPTANCE_REQUIRED" : "PRICING_BASES_EXCEED_TOTAL",
				preview.canCreate() ? "Confira e aceite a prévia atual do rateio antes de criar."
						: "A soma das bases ultrapassa o preço da ordem. Ajuste os dados.");
		this.preview = preview;
	}

	public PricingPreviewOutput preview() {
		return preview;
	}

}
