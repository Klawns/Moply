package com.klaus.moply.workorders.infra.web.controller;

import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;

import com.klaus.moply.workorders.application.usecase.PreviewWorkOrderPricing;

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
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.infra.web.dto.request.CancelRequest;
import com.klaus.moply.workorders.infra.web.dto.request.RescheduleRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

	private final CreateWorkOrder create;

	private final PreviewWorkOrderPricing preview;

	@PostMapping("/pricing-preview")
	public PricingPreviewOutput preview(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestBody CreateWorkOrderInput input) {
		return preview.execute(new Context(principal.getOrganizationId()), input);
	}

	private final FindWorkOrderById findById;

	private final FindWorkOrders find;

	private final CompleteWorkOrder complete;

	private final RescheduleSelectedWorkOrder reschedule;

	private final CancelSelectedWorkOrder cancel;

	@PostMapping("/{id}/complete")
	public ResponseEntity<Void> complete(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		complete.execute(new Context(principal.getOrganizationId()), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/reschedule")
	public ResponseEntity<Void> reschedule(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id,
			@RequestBody RescheduleRequest request) {
		reschedule.execute(new Context(principal.getOrganizationId()),
				new RescheduleSelectedWorkOrderInput(id, principal.getUserId(), request.serviceDate(),
						request.startTime(), request.scope(), request.idempotencyKey()));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<Void> cancel(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id,
			@RequestBody(required = false) CancelRequest request) {
		cancel.execute(new Context(principal.getOrganizationId()), new CancelSelectedWorkOrderInput(id,
				principal.getUserId(), request == null ? null : request.toOptions()));
		return ResponseEntity.noContent().build();
	}

	@PostMapping
	public ResponseEntity<WorkOrderOutput> create(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestBody CreateWorkOrderInput input) {
		var output = create.execute(new Context(principal.getOrganizationId()), input);
		return ResponseEntity.created(URI.create("/api/v1/work-orders/" + output.id())).body(output);
	}

	@GetMapping("/{id}")
	public WorkOrderOutput get(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		return findById.execute(new Context(principal.getOrganizationId()), id);
	}

	@GetMapping
	public PageResponse<WorkOrderOutput> list(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) UUID customerId, @RequestParam(required = false) WorkOrderStatus status,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
			@RequestParam(required = false) String sort, @RequestParam(required = false) String direction) {
		return PageResponse
			.from(find.execute(new Context(principal.getOrganizationId()), new FindWorkOrders.Filter(from, to,
					customerId, status, PageQueryRequest.toQuery(page, size, sort, direction))), value -> value);
	}

}
