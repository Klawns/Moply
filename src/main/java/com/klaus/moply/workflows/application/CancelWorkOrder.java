package com.klaus.moply.workflows.application;

import java.util.UUID;

import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.domain.entity.WorkOrder;

import lombok.RequiredArgsConstructor;

/** Coordinates cancellation through the public work contract. */
@RequiredArgsConstructor
public class CancelWorkOrder implements Usecase.Contextual<UUID, Void> {

	private final WorkOrderOperations operations;

	@Override
	public Void execute(Usecase.Context context, UUID id) {
		operations.update(context.organizationId(), id, WorkOrder::cancel);
		return null;
	}

}
