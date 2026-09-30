package com.klaus.moply.collaborators.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.collaborators.domain.entities.Collaborator;

public interface CollaboratorRepository {

	Collaborator save(UUID organizationId, Collaborator collaborator);

	Optional<Collaborator> findById(UUID organizationId, UUID id);

	List<Collaborator> findAll(UUID organizationId, Boolean active);

}
