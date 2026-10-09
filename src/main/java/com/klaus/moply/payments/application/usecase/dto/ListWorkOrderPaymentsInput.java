package com.klaus.moply.payments.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageQuery;

public record ListWorkOrderPaymentsInput(UUID workOrderId, PageQuery page) {
	public ListWorkOrderPaymentsInput(UUID workOrderId) {
		this(workOrderId, PageQuery.defaults());
	}
}
