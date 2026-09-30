package com.klaus.moply.customers.infra.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID> {

	java.util.Optional<CustomerEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

	java.util.List<CustomerEntity> findAllByOrganizationId(UUID organizationId);

}
