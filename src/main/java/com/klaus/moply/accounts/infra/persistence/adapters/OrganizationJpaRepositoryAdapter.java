package com.klaus.moply.accounts.infra.persistence.adapters;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.accounts.infra.persistence.OrganizationJpaRepository;
import com.klaus.moply.accounts.infra.persistence.entities.OrganizationEntity;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrganizationJpaRepositoryAdapter implements OrganizationRepository {

	private final OrganizationJpaRepository repo;

	@Override
	@Transactional(readOnly = true)
	public Optional<Organization> findById(UUID id) {
		return repo.findById(id).map(OrganizationEntity::toDomain);
	}

	@Override
	@Transactional
	public Organization update(Organization organization) {
		var entity = repo.findById(organization.id()).orElseThrow(AccountNotFoundException::new);
		entity.updatePreferences(organization);
		return repo.save(entity).toDomain();
	}

}
