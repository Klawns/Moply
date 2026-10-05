package com.klaus.moply.collaborators.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;

public interface CollaboratorRepository {

	Collaborator save(UUID organizationId, Collaborator collaborator);

	Optional<Collaborator> findById(UUID organizationId, UUID id);

	PageResult<Collaborator> findAll(UUID organizationId, Boolean active, PageQuery page);

	List<Collaborator> findAllActive(UUID organizationId);

}
