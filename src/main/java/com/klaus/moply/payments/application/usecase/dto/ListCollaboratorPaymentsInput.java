package com.klaus.moply.payments.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageQuery;

public record ListCollaboratorPaymentsInput(UUID workOrderId, UUID collaboratorId, PageQuery page) {
	public ListCollaboratorPaymentsInput(UUID workOrderId, UUID collaboratorId) {
		this(workOrderId, collaboratorId, PageQuery.defaults());
	}
}
