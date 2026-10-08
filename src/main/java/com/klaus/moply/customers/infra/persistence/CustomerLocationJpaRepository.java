package com.klaus.moply.customers.infra.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerLocationJpaRepository extends JpaRepository<CustomerLocationEntity, UUID>,
		org.springframework.data.jpa.repository.JpaSpecificationExecutor<CustomerLocationEntity> {

	@Override
	@org.springframework.data.jpa.repository.EntityGraph(attributePaths = "customer")
	Page<CustomerLocationEntity> findAll(
			org.springframework.data.jpa.domain.Specification<CustomerLocationEntity> specification, Pageable pageable);

	Page<CustomerLocationEntity> findByOrganizationIdAndCustomerId(UUID organizationId, UUID customerId,
			Pageable pageable);

}
