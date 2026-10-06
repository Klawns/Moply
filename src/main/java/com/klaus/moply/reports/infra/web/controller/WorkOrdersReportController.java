package com.klaus.moply.reports.infra.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.reports.application.usecase.FindWorkOrdersReport;
import com.klaus.moply.reports.infra.web.api.WorkOrdersReportApi;
import com.klaus.moply.reports.infra.web.dto.request.ReportPeriodRequest;
import com.klaus.moply.reports.infra.web.dto.response.WorkOrdersReportResponse;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports/work-orders")
@RequiredArgsConstructor
public class WorkOrdersReportController implements WorkOrdersReportApi {

	private final FindWorkOrdersReport report;

	@GetMapping
	@Override
	public WorkOrdersReportResponse get(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @ModelAttribute ReportPeriodRequest request) {
		return WorkOrdersReportResponse
			.from(report.execute(new Context(principal.getOrganizationId()), request.toInput()));
	}

}
