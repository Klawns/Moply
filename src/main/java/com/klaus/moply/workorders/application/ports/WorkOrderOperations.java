package com.klaus.moply.workorders.application.ports;

import java.util.UUID;
import java.util.function.UnaryOperator;

import com.klaus.moply.workorders.domain.entity.WorkOrder;

/**
 * Executes an operational transition under the account-scoped work lock in one
 * transaction. The callback must preserve identity, conditions and assignments.
 */
public interface WorkOrderOperations {

	void update(UUID organizationId, UUID id, UnaryOperator<WorkOrder> transition);

}
