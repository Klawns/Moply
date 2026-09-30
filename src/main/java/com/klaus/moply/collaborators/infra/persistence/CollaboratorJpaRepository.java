package com.klaus.moply.collaborators.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CollaboratorJpaRepository extends JpaRepository<CollaboratorEntity, UUID> {

	Optional<CollaboratorEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

	List<CollaboratorEntity> findAllByOrganizationId(UUID organizationId);

	List<CollaboratorEntity> findAllByOrganizationIdAndActive(UUID organizationId, boolean active);

}
