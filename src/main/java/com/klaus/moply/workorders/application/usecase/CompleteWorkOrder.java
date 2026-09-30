package com.klaus.moply.workorders.application.usecase;

import java.util.UUID;

import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.domain.entity.WorkOrder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CompleteWorkOrder implements Usecase.Contextual<UUID, Void> {

	private final WorkOrderOperations operations;

	@Override
	public Void execute(Usecase.Context context, UUID id) {
		operations.update(context.organizationId(), id, WorkOrder::complete);
		return null;
	}

}
