package com.klaus.moply.payments.infra.web.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.payments.application.usecase.ListCollaboratorPayments;
import com.klaus.moply.payments.application.usecase.RecordCollaboratorPayment;
import com.klaus.moply.payments.application.usecase.ReverseCollaboratorPayment;
import com.klaus.moply.payments.infra.web.api.PaymentWorkOrderCollaboratorApi;
import com.klaus.moply.payments.infra.web.dto.response.PaymentResponse;
import com.klaus.moply.payments.infra.web.dto.request.RecordCollaboratorPaymentRequest;
import com.klaus.moply.payments.infra.web.dto.request.ReverseCollaboratorPaymentRequest;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.infra.web.PageQueryRequest;
import com.klaus.moply.shared.infra.web.dto.PageResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/work-orders/{workOrderId}/collaborators/{collaboratorId}/payments")
@RequiredArgsConstructor
public class PaymentWorkOrderCollaboratorController implements PaymentWorkOrderCollaboratorApi {

	private final RecordCollaboratorPayment record;

	private final ListCollaboratorPayments list;

	private final ReverseCollaboratorPayment reverse;

	@PostMapping
	@Override
	public ResponseEntity<PaymentResponse> record(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID workOrderId, @PathVariable UUID collaboratorId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody RecordCollaboratorPaymentRequest request) {
		var payment = record.execute(new Context(principal.getOrganizationId()),
				new RecordCollaboratorPayment.Input(workOrderId, collaboratorId, request.amount(), request.paidOn(),
						idempotencyKey, principal.getUserId()));
		return ResponseEntity.created(URI.create("/api/v1/payments/" + payment.id()))
			.body(PaymentResponse.from(payment));
	}

	@GetMapping
	@Override
	public PageResponse<PaymentResponse> list(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID workOrderId, @PathVariable UUID collaboratorId,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer size,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String sort,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String direction) {
		return PageResponse.from(
				list.execute(new Context(principal.getOrganizationId()), new ListCollaboratorPayments.Input(workOrderId,
						collaboratorId, PageQueryRequest.toQuery(page, size, sort, direction))),
				PaymentResponse::from);
	}

	@PostMapping("/{paymentId}/reversal")
	@Override
	public PaymentResponse reverse(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID workOrderId,
			@PathVariable UUID collaboratorId, @PathVariable UUID paymentId,
			@Valid @RequestBody ReverseCollaboratorPaymentRequest request) {
		return PaymentResponse.from(reverse.execute(new Context(principal.getOrganizationId()),
				new ReverseCollaboratorPayment.Input(workOrderId, collaboratorId, paymentId,
						request.confirmNotActuallyPaid(), request.reason(), principal.getUserId())));
	}

}
