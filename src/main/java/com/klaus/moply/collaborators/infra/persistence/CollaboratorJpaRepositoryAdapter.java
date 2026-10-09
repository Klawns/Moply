package com.klaus.moply.collaborators.infra.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.infra.persistence.PageableMapper;

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
			.map(entity -> Objects.requireNonNull(entity).toDomain());
	}

	@Override
	public PageResult<Collaborator> findAll(UUID organizationId, Boolean active, PageQuery page) {
		var account = Objects.requireNonNull(organizationId);
		Specification<CollaboratorEntity> specification = (root, query, builder) -> builder
			.equal(root.get("organizationId"), account);
		if (active != null)
			specification = specification.and((root, query, builder) -> builder.equal(root.get("active"), active));
		var result = repo.findAll(specification,
				PageableMapper.toPageable(page, java.util.Set.of("name", "active", "id"), "name"));
		return PageableMapper.toResult(result, entity -> Objects.requireNonNull(entity).toDomain());
	}

	@Override
	public List<Collaborator> findAllActive(UUID organizationId) {
		var account = Objects.requireNonNull(organizationId);
		Specification<CollaboratorEntity> specification = (root, query, builder) -> builder
			.and(builder.equal(root.get("organizationId"), account), builder.isTrue(root.get("active")));
		return repo
			.findAll(specification,
					org.springframework.data.domain.Sort.by("name").and(org.springframework.data.domain.Sort.by("id")))
			.stream()
			.map(entity -> Objects.requireNonNull(entity).toDomain())
			.toList();
	}

}
