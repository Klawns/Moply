package com.klaus.moply.auth.infra.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.klaus.moply.accounts.application.ports.AppUserRepository;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.shared.domain.exception.DomainException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

	private final AppUserRepository users;

	@Override
	public UserDetails loadUserByUsername(String username) {
		try {
			return users.findByEmail(new LoginEmail(username))
				.map(AccountPrincipal::new)
				.orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
		}
		catch (DomainException exception) {
			throw new UsernameNotFoundException("Credenciais inválidas.");
		}
	}

}
