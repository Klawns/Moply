package com.klaus.moply.customers.infra.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerLocationJpaRepository extends JpaRepository<CustomerLocationEntity, UUID> {

	Page<CustomerLocationEntity> findByOrganizationIdAndCustomerId(UUID organizationId, UUID customerId,
			Pageable pageable);

}
