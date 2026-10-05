package com.klaus.moply.workorders.application.ports;

import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;

import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public interface WorkOrderRepository {

	WorkOrder save(UUID organizationId, WorkOrder workOrder);

	Optional<WorkOrder> findById(UUID organizationId, UUID id);

	List<WorkOrder> findAllByCollaborator(UUID organizationId, UUID collaboratorId);

	default List<WorkOrder> findAll(UUID organizationId, WorkOrderDateRange dateRange, UUID customerId) {
		return findAll(organizationId, dateRange, customerId, null);
	}

	List<WorkOrder> findAll(UUID organizationId, WorkOrderDateRange dateRange, UUID customerId, WorkOrderStatus status);

	PageResult<WorkOrder> search(UUID organizationId, WorkOrderDateRange dateRange, UUID customerId,
			WorkOrderStatus status, PageQuery page);

}
