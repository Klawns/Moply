package com.klaus.moply.workorders.infra.web.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.workorders.application.usecase.dto.FindWorkOrdersFilter;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.infra.web.PageQueryRequest;
import com.klaus.moply.shared.infra.web.dto.PageResponse;
import com.klaus.moply.workflows.application.usecase.CancelSelectedWorkOrder;
import com.klaus.moply.workflows.application.usecase.RescheduleSelectedWorkOrder;
import com.klaus.moply.workflows.application.usecase.dto.CancelSelectedWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleSelectedWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.CompleteWorkOrder;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.workorders.application.usecase.FindWorkOrderById;
import com.klaus.moply.workorders.application.usecase.FindWorkOrders;
import com.klaus.moply.workorders.application.usecase.PreviewWorkOrderPricing;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.infra.web.api.WorkOrderApi;
import com.klaus.moply.workorders.infra.web.dto.request.CancelRequest;
import com.klaus.moply.workorders.infra.web.dto.request.CreateWorkOrderRequest;
import com.klaus.moply.workorders.infra.web.dto.request.RescheduleRequest;
import com.klaus.moply.workorders.infra.web.dto.response.PricingPreviewResponse;
import com.klaus.moply.workorders.infra.web.dto.response.WorkOrderResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController implements WorkOrderApi {

	private final CreateWorkOrder create;

	private final PreviewWorkOrderPricing preview;

	private final FindWorkOrderById findById;

	private final FindWorkOrders find;

	private final CompleteWorkOrder complete;

	private final RescheduleSelectedWorkOrder reschedule;

	private final CancelSelectedWorkOrder cancel;

	@PostMapping("/pricing-preview")
	@Override
	public PricingPreviewResponse preview(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody CreateWorkOrderRequest request) {
		return PricingPreviewResponse
			.from(preview.execute(new Context(principal.getOrganizationId()), request.toInput()));
	}

	@PostMapping("/{id}/complete")
	@Override
	public ResponseEntity<Void> complete(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		complete.execute(new Context(principal.getOrganizationId()), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/reschedule")
	@Override
	public ResponseEntity<Void> reschedule(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id,
			@RequestBody RescheduleRequest request) {
		reschedule.execute(new Context(principal.getOrganizationId()),
				new RescheduleSelectedWorkOrderInput(id, principal.getUserId(), request.serviceDate(),
						request.startTime(), request.scope(), request.idempotencyKey()));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/cancel")
	@Override
	public ResponseEntity<Void> cancel(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id,
			@RequestBody(required = false) CancelRequest request) {
		cancel.execute(new Context(principal.getOrganizationId()), new CancelSelectedWorkOrderInput(id,
				principal.getUserId(), request == null ? null : request.toOptions()));
		return ResponseEntity.noContent().build();
	}

	@PostMapping
	@Override
	public ResponseEntity<WorkOrderResponse> create(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody CreateWorkOrderRequest request) {
		var output = create.execute(new Context(principal.getOrganizationId()), request.toInput());
		return ResponseEntity.created(URI.create("/api/v1/work-orders/" + output.id())).body(WorkOrderResponse.from(output));
	}

	@GetMapping("/{id}")
	@Override
	public WorkOrderResponse get(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		return WorkOrderResponse.from(findById.execute(new Context(principal.getOrganizationId()), id));
	}

	@GetMapping
	@Override
	public PageResponse<WorkOrderResponse> list(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) UUID customerId, @RequestParam(required = false) WorkOrderStatus status,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
			@RequestParam(required = false) String sort, @RequestParam(required = false) String direction) {
		return PageResponse
			.from(find.execute(new Context(principal.getOrganizationId()), new FindWorkOrdersFilter(from, to,
					customerId, status, PageQueryRequest.toQuery(page, size, sort, direction))), WorkOrderResponse::from);
	}

}
