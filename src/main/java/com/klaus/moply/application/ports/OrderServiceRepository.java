package com.klaus.moply.application.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.domain.entity.OrderService;

public interface OrderServiceRepository {

	OrderService save(OrderService prestacaoServico);

	Optional<OrderService> findById(UUID id);

	List<OrderService> findByCustomerName(String cliente);

	List<OrderService> findAllByServiceDate(LocalDate serviceDate);

	void deleteById(UUID id);

}
