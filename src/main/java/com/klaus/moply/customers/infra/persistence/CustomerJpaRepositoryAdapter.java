package com.klaus.moply.customers.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerJpaRepositoryAdapter implements CustomerRepository {

	private final CustomerJpaRepository repo;

	private final EntityManager entityManager;

	@Override
	@Transactional
	public Customer save(Customer customer) {
		CustomerEntity entity = customer.getId() == null ? new CustomerEntity(UUID.randomUUID())
				: repo.findById(customer.getId()).orElseThrow(() -> new CustomerNotFoundException(customer.getId()));
		entity.update(customer);
		if (customer.getId() == null) {
			entityManager.persist(entity);
		}
		repo.flush();
		return entity.toDomain();
	}

	@Override
	public Optional<Customer> findById(UUID id) {
		return repo.findById(id).map(CustomerEntity::toDomain);
	}

	@Override
	public List<Customer> findAll() {
		return repo.findAll().stream().map(CustomerEntity::toDomain).toList();
	}

}
