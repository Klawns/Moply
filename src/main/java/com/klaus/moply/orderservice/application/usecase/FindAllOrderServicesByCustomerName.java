package com.klaus.moply.orderservice.application.usecase;

import java.util.List;

import com.klaus.moply.customers.domain.vo.CustomerName;
import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.application.usecase.dto.OrderServiceOutput;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllOrderServicesByCustomerName implements Usecase<CustomerName, List<OrderServiceOutput>> {

	private final OrderServiceRepository repo;

	public List<OrderServiceOutput> execute(CustomerName customer) {
		return repo.findByCustomerName(customer.value()).stream().map(OrderServiceOutput::fromDomain).toList();
	}

}
