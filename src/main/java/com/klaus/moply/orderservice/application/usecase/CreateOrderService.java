package com.klaus.moply.orderservice.application.usecase;

import java.util.UUID;

import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.application.usecase.dto.CreateOrderServiceInput;
import com.klaus.moply.orderservice.domain.entity.OrderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderService implements Usecase<CreateOrderServiceInput, UUID> {

	private final OrderServiceRepository repo;

	public UUID execute(CreateOrderServiceInput input) {
		OrderService orderService = OrderService.create(input.customer(), input.contractedHours(), input.HourlyPrice(),
				input.employeeCount(), input.serviceDate());

		orderService = repo.save(orderService);

		return orderService.getId();
	}

}
