package com.klaus.moply.application.usecase;

import java.util.List;

import com.klaus.moply.application.dto.OrderServiceOutput;
import com.klaus.moply.application.ports.OrderServiceRepository;
import com.klaus.moply.domain.vo.Customer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllOrderServicesByCustomerName implements Usecase<Customer, List<OrderServiceOutput>> {

    private final OrderServiceRepository repo;

    public List<OrderServiceOutput> execute(Customer customer) {
        return repo.findByCustomerName(customer.name()).stream()
                .map(OrderServiceOutput::fromDomain)
                .toList();
    }
}
