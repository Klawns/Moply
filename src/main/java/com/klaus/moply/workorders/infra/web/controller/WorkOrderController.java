package com.klaus.moply.workorders.infra.web.controller;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
import com.klaus.moply.workflows.application.CancelWorkOrder;
import com.klaus.moply.workorders.application.usecase.CompleteWorkOrder;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.workorders.application.usecase.FindWorkOrderById;
import com.klaus.moply.workorders.application.usecase.FindWorkOrders;
import com.klaus.moply.workorders.application.usecase.RescheduleWorkOrder;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

	private final CreateWorkOrder create;

	private final FindWorkOrderById findById;

	private final FindWorkOrders find;

	private final CompleteWorkOrder complete;

	private final RescheduleWorkOrder reschedule;

	private final CancelWorkOrder cancel;

	public record RescheduleRequest(LocalDate serviceDate, LocalTime startTime) {
	}

	@PostMapping("/{id}/complete")
	public ResponseEntity<Void> complete(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		complete.execute(new Context(principal.getOrganizationId()), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/reschedule")
	public ResponseEntity<Void> reschedule(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id,
			@RequestBody RescheduleRequest request) {
		reschedule.execute(new Context(principal.getOrganizationId()),
				new RescheduleWorkOrder.Input(id, request.serviceDate(), request.startTime()));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<Void> cancel(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		cancel.execute(new Context(principal.getOrganizationId()), id);
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
	public List<WorkOrderOutput> list(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) UUID customerId, @RequestParam(required = false) WorkOrderStatus status) {
		return find.execute(new Context(principal.getOrganizationId()),
				new FindWorkOrders.Filter(from, to, customerId, status));
	}

}
