package com.klaus.moply.workorders.application.usecase.dto;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;

public record FindWorkOrdersFilter(WorkOrderDateRange dateRange, UUID customerId, WorkOrderStatus status,
		PageQuery page) {
	public FindWorkOrdersFilter(LocalDate from, LocalDate to, UUID customerId) {
		this(from, to, customerId, null, PageQuery.defaults());
	}

	public FindWorkOrdersFilter(LocalDate from, LocalDate to, UUID customerId, WorkOrderStatus status) {
		this(new WorkOrderDateRange(from, to), customerId, status, PageQuery.defaults());
	}

	public FindWorkOrdersFilter(LocalDate from, LocalDate to, UUID customerId, WorkOrderStatus status, PageQuery page) {
		this(new WorkOrderDateRange(from, to), customerId, status, page);
	}

	public FindWorkOrdersFilter {
		Objects.requireNonNull(dateRange);
		Objects.requireNonNull(page);
	}
}
