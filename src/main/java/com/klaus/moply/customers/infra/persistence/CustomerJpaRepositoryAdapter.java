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
	public Customer save(UUID organizationId, Customer customer) {
		java.util.Objects.requireNonNull(organizationId, "organizationId");
		CustomerEntity entity = customer.getId() == null ? new CustomerEntity(UUID.randomUUID(), organizationId)
				: repo.findByOrganizationIdAndId(organizationId, customer.getId())
					.orElseThrow(() -> new CustomerNotFoundException(customer.getId()));
		if (customer.getId() != null) {
			if (!organizationId.equals(customer.getOrganizationId()) || entity.getVersion() != customer.getVersion()) {
				throw new org.springframework.orm.ObjectOptimisticLockingFailureException(CustomerEntity.class,
						customer.getId());
			}
			// Force a root version check even when only a child changes.
			int changed = entityManager.createQuery(
					"update CustomerEntity c set c.version = c.version + 1 where c.id = :id and c.organizationId = :account and c.version = :version")
				.setParameter("id", customer.getId())
				.setParameter("account", organizationId)
				.setParameter("version", customer.getVersion())
				.executeUpdate();
			if (changed != 1)
				throw new org.springframework.orm.ObjectOptimisticLockingFailureException(CustomerEntity.class,
						customer.getId());
			entityManager.refresh(entity);
		}
		entity.update(customer);
		if (customer.getId() == null) {
			entityManager.persist(entity);
		}
		repo.flush();
		return entity.toDomain();
	}

	@Override
	public Optional<Customer> findById(UUID organizationId, UUID id) {
		return repo.findByOrganizationIdAndId(java.util.Objects.requireNonNull(organizationId), id)
			.map(CustomerEntity::toDomain);
	}

	@Override
	public List<Customer> findAll(UUID organizationId) {
		return repo.findAllByOrganizationId(java.util.Objects.requireNonNull(organizationId))
			.stream()
			.map(CustomerEntity::toDomain)
			.toList();
	}

}
