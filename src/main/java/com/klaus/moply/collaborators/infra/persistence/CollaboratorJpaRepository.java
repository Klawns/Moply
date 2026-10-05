package com.klaus.moply.collaborators.infra.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CollaboratorJpaRepository
		extends JpaRepository<CollaboratorEntity, UUID>, JpaSpecificationExecutor<CollaboratorEntity> {

	Optional<CollaboratorEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

}
