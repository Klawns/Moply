package com.klaus.moply.workorders.application.ports;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.klaus.moply.workorders.domain.entity.WorkOrder;

/** Operations protected by the account-scoped work order lock in one transaction. */
public interface WorkOrderOperations {

	/** Applies a transition that preserves identity, conditions and assignments. */
	void update(UUID organizationId, UUID id, UnaryOperator<WorkOrder> transition);

	/**
	 * Runs an operation against the account-scoped work order while holding its
	 * operational lock in a transaction. Use this when the operation must return a result
	 * without transitioning the work order.
	 */
	<T> T withWorkOrder(UUID organizationId, UUID id, Function<WorkOrder, T> operation);

}
