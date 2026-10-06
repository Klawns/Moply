package com.klaus.moply.reports.infra.web.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReportInput;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.shared.infra.web.PageQueryRequest;

import jakarta.validation.constraints.NotNull;

public record CollaboratorReportPeriodRequest(@NotNull LocalDate from, @NotNull LocalDate to, UUID customerId,
		UUID collaboratorId, Integer page, Integer size, String sort, String direction) {

	public CollaboratorsReportInput toInput() {
		return new CollaboratorsReportInput(
				new ReportPeriod(from, to, customerId, PageQueryRequest.toQuery(page, size, sort, direction)),
				collaboratorId);
	}

}
