package com.klaus.moply.orderservice.application.usecase;

import java.util.List;

import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.application.usecase.dto.OrderServiceOutput;
import com.klaus.moply.orderservice.domain.vo.Customer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllOrderServicesByCustomerName implements Usecase<Customer, List<OrderServiceOutput>> {

	private final OrderServiceRepository repo;

	public List<OrderServiceOutput> execute(Customer customer) {
		return repo.findByCustomerName(customer.name()).stream().map(OrderServiceOutput::fromDomain).toList();
	}

}
