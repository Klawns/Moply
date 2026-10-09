package com.klaus.moply.accounts.infra.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.klaus.moply.accounts.infra.persistence.entities.OrganizationEntity;

import jakarta.persistence.LockModeType;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select organization from OrganizationEntity organization where organization.id = :id")
	Optional<OrganizationEntity> findByIdForUpdate(@Param("id") UUID id);

}
