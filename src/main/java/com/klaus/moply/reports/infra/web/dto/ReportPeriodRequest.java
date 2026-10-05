package com.klaus.moply.reports.infra.web.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.shared.infra.web.PageQueryRequest;

import jakarta.validation.constraints.NotNull;

public record ReportPeriodRequest(@NotNull LocalDate from, @NotNull LocalDate to, UUID customerId, Integer page,
		Integer size, String sort, String direction) {
	public ReportPeriod toInput() {
		return new ReportPeriod(from, to, customerId, PageQueryRequest.toQuery(page, size, sort, direction));
	}
}
