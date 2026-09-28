package com.klaus.moply.orderservice.application.usecase;

import java.time.LocalDate;
import java.util.List;

import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.application.usecase.dto.OrderServiceOutput;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindOrderServiceByServiceDate implements Usecase<LocalDate, List<OrderServiceOutput>> {

	private final OrderServiceRepository repo;

	public List<OrderServiceOutput> execute(LocalDate serviceDate) {
		return repo.findAllByServiceDate(serviceDate).stream().map(OrderServiceOutput::fromDomain).toList();
	}

}
