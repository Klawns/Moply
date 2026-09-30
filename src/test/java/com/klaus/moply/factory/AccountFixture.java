package com.klaus.moply.factory;

import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

public final class AccountFixture {

	public static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-000000000001");

	public static Context context() {
		return new Context(ACCOUNT);
	}

	public static void authenticate() {
		var principal = new AccountPrincipal(
				new AppUser(UUID.randomUUID(), ACCOUNT, new LoginEmail("test@example.com"), "encoded"));
		SecurityContextHolder.getContext()
			.setAuthentication(
					UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
	}

}
