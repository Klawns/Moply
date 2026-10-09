package com.klaus.moply.payments.infra.web.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.payments.application.usecase.dto.ReverseWorkOrderPaymentInput;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.payments.application.usecase.ReverseWorkOrderPayment;
import com.klaus.moply.payments.infra.web.api.PaymentApi;
import com.klaus.moply.payments.infra.web.dto.response.PaymentResponse;
import com.klaus.moply.payments.infra.web.dto.request.ReversePaymentRequest;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

	private final ReverseWorkOrderPayment reverse;

	@PostMapping("/{id}/reversal")
	@Override
	public PaymentResponse reverse(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id,
			@Valid @RequestBody ReversePaymentRequest request) {
		return PaymentResponse
			.from(reverse.execute(new Context(principal.getOrganizationId()), new ReverseWorkOrderPaymentInput(id,
					request.confirmNoMoneyReceived(), request.reason(), principal.getUserId())));
	}

}
