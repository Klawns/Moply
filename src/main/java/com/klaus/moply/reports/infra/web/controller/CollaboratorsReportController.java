package com.klaus.moply.reports.infra.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.reports.application.usecase.FindCollaboratorsReport;
import com.klaus.moply.reports.infra.web.api.CollaboratorsReportApi;
import com.klaus.moply.reports.infra.web.dto.request.CollaboratorReportPeriodRequest;
import com.klaus.moply.reports.infra.web.dto.response.CollaboratorsReportResponse;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports/collaborators")
@RequiredArgsConstructor
public class CollaboratorsReportController implements CollaboratorsReportApi {

	private final FindCollaboratorsReport report;

	@GetMapping
	@Override
	public CollaboratorsReportResponse get(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @ModelAttribute CollaboratorReportPeriodRequest request) {
		return CollaboratorsReportResponse
			.from(report.execute(new Context(principal.getOrganizationId()), request.toInput()));
	}

}
