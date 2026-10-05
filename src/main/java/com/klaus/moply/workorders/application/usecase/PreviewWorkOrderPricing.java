package com.klaus.moply.workorders.application.usecase;

import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PreviewWorkOrderPricing implements Usecase.Contextual<CreateWorkOrderInput, PricingPreviewOutput> {

	private final CreateWorkOrder create;

	@Override
	public PricingPreviewOutput execute(Usecase.Context context, CreateWorkOrderInput input) {
		return create.preview(context, input);
	}

}
