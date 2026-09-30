package com.klaus.moply.accounts.application.ports;

import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.accounts.domain.vo.Organization;

public interface OrganizationRepository {

	Optional<Organization> findById(UUID id);

	Organization update(Organization organization);

}
