package com.klaus.moply.orderservice.application.usecase;

import java.util.UUID;

import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.application.usecase.dto.OrderServiceOutput;
import com.klaus.moply.orderservice.application.usecase.exception.OrderServiceNotFoundException;
import com.klaus.moply.orderservice.domain.entity.OrderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindOrderServiceById implements Usecase<UUID, OrderServiceOutput> {

	private final OrderServiceRepository repo;

	public OrderServiceOutput execute(UUID id) {
		OrderService orderService = repo.findById(id).orElseThrow(() -> new OrderServiceNotFoundException(id));

		return OrderServiceOutput.fromDomain(orderService);
	}

}
