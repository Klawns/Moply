package com.klaus.moply.workorders.application.usecase;

import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;
import lombok.RequiredArgsConstructor;
import com.klaus.moply.workorders.application.service.WorkOrderPreparation;

@RequiredArgsConstructor
public class PreviewWorkOrderPricing implements Usecase.Contextual<CreateWorkOrderInput, PricingPreviewOutput> {

	private final WorkOrderPreparation preparation;

	@Override
	public PricingPreviewOutput execute(Usecase.Context context, CreateWorkOrderInput input) {
		return preparation.preview(context, input);
	}

}
