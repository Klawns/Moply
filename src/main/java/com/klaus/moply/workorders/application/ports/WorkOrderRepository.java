package com.klaus.moply.workorders.application.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public interface WorkOrderRepository {

	WorkOrder save(UUID organizationId, WorkOrder workOrder);

	Optional<WorkOrder> findById(UUID organizationId, UUID id);

	default List<WorkOrder> findAll(UUID organizationId, LocalDate from, LocalDate to, UUID customerId) {
		return findAll(organizationId, from, to, customerId, null);
	}

	List<WorkOrder> findAll(UUID organizationId, LocalDate from, LocalDate to, UUID customerId, WorkOrderStatus status);

}
