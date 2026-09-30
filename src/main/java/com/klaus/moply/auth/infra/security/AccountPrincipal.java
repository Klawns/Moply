package com.klaus.moply.auth.infra.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.klaus.moply.accounts.domain.entities.AppUser;

import lombok.Getter;

@Getter
public class AccountPrincipal extends User {

	private final UUID userId;

	private final UUID organizationId;

	public AccountPrincipal(AppUser user) {
		super(user.getEmail().value(), user.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
		userId = user.getId();
		organizationId = user.getOrganizationId();
	}

}
