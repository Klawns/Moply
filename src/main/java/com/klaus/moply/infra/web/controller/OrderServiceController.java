package com.klaus.moply.infra.web.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.application.usecase.CreateOrderService;
import com.klaus.moply.application.usecase.DeleteOrderService;
import com.klaus.moply.application.usecase.FindAllOrderServicesByCustomerName;
import com.klaus.moply.application.usecase.FindOrderServiceById;
import com.klaus.moply.application.usecase.FindOrderServiceByServiceDate;
import com.klaus.moply.application.usecase.dto.CreateOrderServiceInput;
import com.klaus.moply.application.usecase.dto.OrderServiceOutput;
import com.klaus.moply.domain.vo.Customer;
import com.klaus.moply.infra.web.dto.request.CreateOrderServiceRequest;
import com.klaus.moply.infra.web.dto.response.OrderServiceResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/order-services")
@RequiredArgsConstructor
public class OrderServiceController {

	private final CreateOrderService create;

	private final FindOrderServiceByServiceDate findByServiceDate;

	private final FindAllOrderServicesByCustomerName findByCustomer;

	private final FindOrderServiceById findById;

	private final DeleteOrderService delete;

	@PostMapping
	public ResponseEntity<UUID> create(@RequestBody @Valid CreateOrderServiceRequest request) {

		CreateOrderServiceInput input = CreateOrderServiceInput.builder()
				.customer(request.customer())
				.contractedHours(request.contractedHours())
				.HourlyPrice(request.hourlyRate())
				.employeeCount(request.employeeCount())
				.serviceDate(request.serviceDate())
				.build();

		UUID response = create.execute(input);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<OrderServiceResponse>> list(@RequestParam LocalDate serviceDate,
			@RequestParam(required = false) String customer) {

		List<OrderServiceOutput> orderService;

		if (customer != null) {
			orderService = findByCustomer.execute(new Customer(customer));
		} else {
			orderService = findByServiceDate.execute(serviceDate);
		}

		List<OrderServiceResponse> response = orderService.stream().map(OrderServiceResponse::from).toList();

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{orderId}")
	public ResponseEntity<OrderServiceResponse> findById(@PathVariable UUID orderId) {

		OrderServiceOutput output = findById.execute(orderId);

		return ResponseEntity.ok(OrderServiceResponse.from(output));
	}

	@DeleteMapping("/{orderId}")
	public ResponseEntity<Void> delete(@PathVariable UUID orderId) {
		delete.execute(orderId);

		return ResponseEntity.noContent().build();
	}

}
