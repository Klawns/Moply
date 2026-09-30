package com.klaus.moply.accounts.infra.persistence.adapters;

import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.accounts.application.exception.AccountConflictException;
import com.klaus.moply.accounts.application.ports.AccountRegistration;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.accounts.infra.persistence.AppUserJpaRepository;
import com.klaus.moply.accounts.infra.persistence.OrganizationJpaRepository;
import com.klaus.moply.accounts.infra.persistence.entities.AppUserEntity;
import com.klaus.moply.accounts.infra.persistence.entities.OrganizationEntity;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AccountRegistrationJpaAdapter implements AccountRegistration {

	private final OrganizationJpaRepository organizations;

	private final AppUserJpaRepository users;

	@Override
	@Transactional
	public void register(Organization organization, AppUser manager) {
		if (!organization.id().equals(manager.getOrganizationId())) {
			throw new IllegalArgumentException("O gestor deve pertencer à conta cadastrada.");
		}
		try {
			var entity = organizations.saveAndFlush(new OrganizationEntity(organization));
			users.saveAndFlush(new AppUserEntity(manager, entity));
		}
		catch (DataIntegrityViolationException exception) {
			for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
				if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
					throw new AccountConflictException();
				}
			}
			throw exception;
		}
	}

}
