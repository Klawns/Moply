package com.klaus.moply.accounts.domain.entities;

import java.util.UUID;

import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.shared.domain.exception.DomainException;

import lombok.Getter;

@Getter
public final class AppUser {

	private final UUID id;

	private final UUID organizationId;

	private final LoginEmail email;

	private final String passwordHash;

	public AppUser(UUID id, UUID organizationId, LoginEmail email, String passwordHash) {
		if (id == null || organizationId == null || email == null || passwordHash == null || passwordHash.isBlank()) {
			throw new DomainException("Gestor inválido.");
		}
		this.id = id;
		this.organizationId = organizationId;
		this.email = email;
		this.passwordHash = passwordHash;
	}

}
