package com.klaus.moply.payments.infra.web.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.payments.application.usecase.ListWorkOrderPayments;
import com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment;
import com.klaus.moply.payments.infra.web.dto.PaymentResponse;
import com.klaus.moply.payments.infra.web.dto.RecordPaymentRequest;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.infra.web.PageQueryRequest;
import com.klaus.moply.shared.infra.web.dto.PageResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/work-orders/{workOrderId}/payments")
@RequiredArgsConstructor
public class PaymentWorkOrderController {

	private final RecordWorkOrderPayment record;

	private final ListWorkOrderPayments list;

	@PostMapping
	public ResponseEntity<PaymentResponse> record(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID workOrderId, @Valid @RequestBody RecordPaymentRequest request) {
		var payment = record.execute(new Context(principal.getOrganizationId()),
				new RecordWorkOrderPayment.Input(workOrderId, request.paidOn(), principal.getUserId()));
		return ResponseEntity.created(URI.create("/api/v1/payments/" + payment.id()))
			.body(PaymentResponse.from(payment));
	}

	@GetMapping
	public PageResponse<PaymentResponse> list(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID workOrderId,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer size,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String sort,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String direction) {
		return PageResponse.from(list.execute(new Context(principal.getOrganizationId()),
				new ListWorkOrderPayments.Input(workOrderId, PageQueryRequest.toQuery(page, size, sort, direction))),
				PaymentResponse::from);
	}

}
