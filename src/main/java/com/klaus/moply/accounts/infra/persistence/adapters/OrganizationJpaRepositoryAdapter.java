package com.klaus.moply.accounts.infra.persistence.adapters;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.accounts.infra.persistence.OrganizationJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrganizationJpaRepositoryAdapter implements OrganizationRepository {

	private final OrganizationJpaRepository repo;

	@Override
	@Transactional(readOnly = true)
	public Optional<Organization> findById(UUID id) {
		return repo.findById(id).map(entity -> Objects.requireNonNull(entity).toDomain());
	}

	@Override
	@Transactional
	public Organization updatePreferences(UUID id, UnaryOperator<Organization> change) {
		var entity = repo.findByIdForUpdate(id).orElseThrow(AccountNotFoundException::new);
		entity.updatePreferences(change.apply(entity.toDomain()));
		return repo.save(entity).toDomain();
	}

}
