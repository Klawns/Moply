package com.klaus.moply.reports.infra.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.reports.application.usecase.FindCustomerPaymentsReport;
import com.klaus.moply.reports.infra.web.dto.CustomerPaymentsReportResponse;
import com.klaus.moply.reports.infra.web.dto.ReportPeriodRequest;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports/customer-payments")
@RequiredArgsConstructor
@Validated
public class CustomerPaymentsReportController {

	private final FindCustomerPaymentsReport report;

	@GetMapping
	public CustomerPaymentsReportResponse get(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @ModelAttribute ReportPeriodRequest request) {
		return CustomerPaymentsReportResponse
			.from(report.execute(new Context(principal.getOrganizationId()), request.toInput()));
	}

}
