package com.klaus.moply.accounts.infra.persistence.adapters;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.accounts.application.ports.AppUserRepository;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.accounts.infra.persistence.AppUserJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AppUserJpaRepositoryAdapter implements AppUserRepository {

	private final AppUserJpaRepository repo;

	@Override
	@Transactional(readOnly = true)
	public Optional<AppUser> findById(java.util.UUID id) {
		return repo.findById(id).map(entity -> Objects.requireNonNull(entity).toDomain());
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AppUser> findByEmail(LoginEmail email) {
		return repo.findByEmail(email.value()).map(entity -> Objects.requireNonNull(entity).toDomain());
	}

}
