package com.klaus.moply.orderservice.application.usecase;

import java.util.UUID;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.application.usecase.dto.CreateOrderServiceInput;
import com.klaus.moply.orderservice.domain.entity.OrderService;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderService implements Usecase<CreateOrderServiceInput, UUID> {

	private final OrderServiceRepository repo;

	private final CustomerRepository customers;

	public UUID execute(CreateOrderServiceInput input) {
		if (input.customerId() == null) {
			throw new DomainException("O ID do cliente é obrigatório.");
		}
		var customer = customers.findById(input.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(input.customerId()));
		OrderService orderService = OrderService.create(customer, input.contractedHours(), input.HourlyPrice(),
				input.employeeCount(), input.serviceDate());

		orderService = repo.save(orderService);

		return orderService.getId();
	}

}
