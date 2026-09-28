package com.klaus.moply.orderservice.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.domain.entity.OrderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
@Transactional
public class OrderServiceJpaRepositoryAdapter implements OrderServiceRepository {

	private final OrderServiceJpaRepository repo;

	private final CustomerRepository customers;

	@Override
	public OrderService save(OrderService orderService) {
		OrderServiceEntity entity = OrderServiceEntity.fromDomain(orderService);
		return toDomain(repo.save(entity));
	}

	@Override
	public Optional<OrderService> findById(UUID id) {
		return repo.findById(id).map(this::toDomain);
	}

	@Override
	public List<OrderService> findByCustomerName(String customer) {
		return repo.findAllByCustomerName(customer).stream().map(this::toDomain).toList();
	}

	@Override
	public List<OrderService> findAllByServiceDate(LocalDate serviceDate) {
		return repo.findAllByServiceDate(serviceDate).stream().map(this::toDomain).toList();
	}

	@Override
	public void deleteById(UUID id) {
		repo.deleteById(id);
	}

	private OrderService toDomain(OrderServiceEntity entity) {
		var customer = customers.findById(entity.getCustomerId())
			.orElseThrow(() -> new CustomerNotFoundException(entity.getCustomerId()));
		return entity.toDomain(customer);
	}

}
