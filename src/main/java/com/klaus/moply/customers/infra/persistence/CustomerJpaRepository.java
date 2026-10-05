package com.klaus.moply.customers.infra.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerJpaRepository
		extends JpaRepository<CustomerEntity, UUID>, JpaSpecificationExecutor<CustomerEntity> {

	java.util.Optional<CustomerEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

	boolean existsByOrganizationIdAndId(UUID organizationId, UUID id);

}
