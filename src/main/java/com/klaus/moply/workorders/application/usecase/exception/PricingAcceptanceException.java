package com.klaus.moply.workorders.application.usecase.exception;

import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;

public class PricingAcceptanceException extends RuntimeException {

	private final PricingPreviewOutput preview;

	public PricingAcceptanceException(PricingPreviewOutput preview) {
		super(preview.canCreate() ? "Confira e aceite a prévia atual do rateio antes de criar."
				: "A soma das bases ultrapassa o preço da ordem. Ajuste os dados.");
		this.preview = preview;
	}

	public PricingPreviewOutput preview() {
		return preview;
	}

}
