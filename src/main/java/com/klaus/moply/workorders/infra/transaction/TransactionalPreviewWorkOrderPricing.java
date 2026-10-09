package com.klaus.moply.workorders.infra.transaction;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.service.WorkOrderPreparation;
import com.klaus.moply.workorders.application.usecase.PreviewWorkOrderPricing;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;

public class TransactionalPreviewWorkOrderPricing extends PreviewWorkOrderPricing {

	public TransactionalPreviewWorkOrderPricing(WorkOrderPreparation preparation) {
		super(preparation);
	}

	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public PricingPreviewOutput execute(Usecase.Context context, CreateWorkOrderInput input) {
		return super.execute(context, input);
	}

}
