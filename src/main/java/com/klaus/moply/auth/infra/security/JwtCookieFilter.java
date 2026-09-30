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

	public JwtCookieFilter(JwtCookieService jwt) {
		this.jwt = jwt;
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
		}
		chain.doFilter(request, response);
	}

}
