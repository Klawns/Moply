package com.klaus.moply.payments.infra.web.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.payments.application.usecase.GetCollaboratorPaymentSummary;
import com.klaus.moply.payments.infra.web.api.PaymentCollaboratorApi;
import com.klaus.moply.payments.infra.web.dto.response.CollaboratorPaymentSummaryResponse;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/collaborators/{collaboratorId}/payments")
@RequiredArgsConstructor
public class PaymentCollaboratorController implements PaymentCollaboratorApi {

	private final GetCollaboratorPaymentSummary summary;

	@GetMapping("/summary")
	@Override
	public CollaboratorPaymentSummaryResponse summary(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID collaboratorId) {
		return CollaboratorPaymentSummaryResponse
			.from(summary.execute(new Context(principal.getOrganizationId()), collaboratorId));
	}

}
