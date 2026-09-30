package com.klaus.moply.accounts.infra.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klaus.moply.accounts.infra.persistence.entities.OrganizationEntity;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationEntity, UUID> {

}
