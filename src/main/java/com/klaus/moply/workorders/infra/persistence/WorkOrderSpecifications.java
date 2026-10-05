package com.klaus.moply.workorders.infra.persistence;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;

final class WorkOrderSpecifications {

	private WorkOrderSpecifications() {
	}

	static Specification<WorkOrderEntity> filters(UUID organizationId, WorkOrderDateRange dateRange, UUID customerId,
			WorkOrderStatus status) {
		Objects.requireNonNull(organizationId);
		Specification<WorkOrderEntity> specification = (root, query, builder) -> builder
			.equal(root.get("organizationId"), organizationId);
		if (dateRange.from() != null)
			specification = specification
				.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("serviceDate"), dateRange.from()));
		if (dateRange.to() != null)
			specification = specification
				.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("serviceDate"), dateRange.to()));
		if (customerId != null)
			specification = specification
				.and((root, query, builder) -> builder.equal(root.get("customerId"), customerId));
		if (status != null)
			specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
		return specification;
	}

}