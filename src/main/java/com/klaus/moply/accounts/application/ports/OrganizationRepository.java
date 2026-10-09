package com.klaus.moply.accounts.application.ports;

import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.klaus.moply.accounts.domain.vo.Organization;

public interface OrganizationRepository {

	Optional<Organization> findById(UUID id);

	/** Applies the preferences change to the current organization atomically. */
	Organization updatePreferences(UUID id, UnaryOperator<Organization> change);

}
