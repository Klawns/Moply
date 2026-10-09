package com.klaus.moply.auth.infra.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtCookieFilter extends OncePerRequestFilter {

	private final JwtCookieService jwt;

	private final tools.jackson.databind.ObjectMapper mapper;

	public JwtCookieFilter(JwtCookieService jwt, tools.jackson.databind.ObjectMapper mapper) {
		this.jwt = jwt;
		this.mapper = mapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String token = null;
		boolean duplicate = false;
		if (request.getCookies() != null)
			for (var cookie : request.getCookies()) {
				if (JwtCookieService.COOKIE.equals(cookie.getName())) {
					if (token != null)
						duplicate = true;
					token = cookie.getValue();
				}
			}
		if (token != null && !duplicate) {
			try {
				var principal = jwt.authenticate(token);
				var context = SecurityContextHolder.createEmptyContext();
				context.setAuthentication(
						UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
				SecurityContextHolder.setContext(context);
			}
			catch (org.springframework.security.oauth2.jwt.JwtException | IllegalArgumentException exception) {
				SecurityContextHolder.clearContext();
			}
			catch (org.springframework.dao.DataAccessException
					| org.springframework.transaction.TransactionException exception) {
				SecurityContextHolder.clearContext();
				SecurityProblemWriter.write(response, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
						"Autenticação indisponível. Tente novamente.", "AUTH_STORAGE_UNAVAILABLE", mapper);
				return;
			}
		}
		chain.doFilter(request, response);
	}

}
