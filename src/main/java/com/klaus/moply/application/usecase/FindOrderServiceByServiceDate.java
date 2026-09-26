package com.klaus.moply.application.usecase;

import java.time.LocalDate;
import java.util.List;

import com.klaus.moply.application.ports.OrderServiceRepository;
import com.klaus.moply.application.usecase.dto.OrderServiceOutput;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindOrderServiceByServiceDate implements Usecase<LocalDate, List<OrderServiceOutput>> {

	private final OrderServiceRepository repo;

	public List<OrderServiceOutput> execute(LocalDate serviceDate) {
		return repo.findAllByServiceDate(serviceDate).stream().map(OrderServiceOutput::fromDomain).toList();
	}

}
