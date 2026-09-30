package com.klaus.moply.collaborators.infra.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.collaborators.application.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollaboratorJpaRepositoryAdapter implements CollaboratorRepository {

	private final CollaboratorJpaRepository repo;

	@Override
	@Transactional
	public Collaborator save(UUID organizationId, Collaborator collaborator) {
		Objects.requireNonNull(organizationId, "organizationId");
		if (!organizationId.equals(collaborator.getOrganizationId()))
			throw new CollaboratorNotFoundException(collaborator.getId());
		var entity = collaborator.getId() == null ? new CollaboratorEntity(organizationId)
				: repo.findByOrganizationIdAndId(organizationId, collaborator.getId())
					.orElseThrow(() -> new CollaboratorNotFoundException(collaborator.getId()));
		if (collaborator.getId() != null) {
			if (entity.getVersion() != collaborator.getVersion())
				throw new ObjectOptimisticLockingFailureException(CollaboratorEntity.class, collaborator.getId());
			if (!entity.isActive())
				throw new InactiveCollaboratorException();
		}
		entity.update(collaborator);
		return repo.saveAndFlush(entity).toDomain();
	}

	@Override
	public Optional<Collaborator> findById(UUID organizationId, UUID id) {
		return repo.findByOrganizationIdAndId(Objects.requireNonNull(organizationId), id)
			.map(CollaboratorEntity::toDomain);
	}

	@Override
	public List<Collaborator> findAll(UUID organizationId, Boolean active) {
		Objects.requireNonNull(organizationId);
		var entities = active == null ? repo.findAllByOrganizationId(organizationId)
				: repo.findAllByOrganizationIdAndActive(organizationId, active);
		return entities.stream().map(CollaboratorEntity::toDomain).toList();
	}

}
