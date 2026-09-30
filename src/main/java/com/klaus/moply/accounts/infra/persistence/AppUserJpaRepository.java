package com.klaus.moply.accounts.infra.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klaus.moply.accounts.infra.persistence.entities.AppUserEntity;

public interface AppUserJpaRepository extends JpaRepository<AppUserEntity, UUID> {

	Optional<AppUserEntity> findByEmail(String email);

}
