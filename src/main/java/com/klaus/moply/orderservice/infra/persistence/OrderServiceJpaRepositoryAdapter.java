package com.klaus.moply.orderservice.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.domain.entity.OrderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class OrderServiceJpaRepositoryAdapter implements OrderServiceRepository {

	private final OrderServiceJpaRepository repo;

	@Override
	public OrderService save(OrderService orderService) {
		OrderServiceEntity entity = OrderServiceEntity.fromDomain(orderService);
		return repo.save(entity).toDomain();
	}

	@Override
	public Optional<OrderService> findById(UUID id) {
		return repo.findById(id).map(entity -> entity.toDomain());
	}

	@Override
	public List<OrderService> findByCustomerName(String customer) {
		return repo.findAllByCustomer(customer).stream().map(entity -> entity.toDomain()).toList();
	}

	@Override
	public List<OrderService> findAllByServiceDate(LocalDate serviceDate) {
		return repo.findAllByServiceDate(serviceDate).stream().map(entity -> entity.toDomain()).toList();
	}

	@Override
	public void deleteById(UUID id) {
		repo.deleteById(id);
	}

}
