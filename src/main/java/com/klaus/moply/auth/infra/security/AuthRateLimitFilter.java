package com.klaus.moply.auth.infra.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import com.klaus.moply.auth.infra.config.AuthRateLimitProperties;
import com.klaus.moply.auth.infra.security.AuthRateLimitStore.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class AuthRateLimitFilter extends OncePerRequestFilter {

	private final AuthRateLimitStore store;

	private final AuthRateLimitProperties policy;

	private final Clock clock;

	private final ObjectMapper mapper;

	private final ClientIpResolver clientIp;

	public AuthRateLimitFilter(AuthRateLimitStore store, AuthRateLimitProperties policy, Clock clock,
			ObjectMapper mapper) {
		this.store = store;
		this.policy = policy;
		this.clock = clock;
		this.mapper = mapper;
		this.clientIp = new ClientIpResolver(policy.trustedProxies());
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equals(request.getMethod())
				|| !("/api/v1/auth/login".equals(path(request)) || "/api/v1/accounts".equals(path(request)));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		// Only an explicitly trusted socket peer may provide X-Real-IP.
		String ip = digest(clientIp.resolve(request));
		List<Bucket> keys;
		if ("/api/v1/auth/login".equals(path(request))) {
			String email = request.getParameter("email");
			email = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
			keys = List.of(new Bucket("login-ip:" + ip, policy.loginIpBurst(), policy.loginIpRefillSeconds()),
					new Bucket("login-account:" + digest(email), policy.loginAccountBurst(),
							policy.loginAccountRefillSeconds()));
		}
		else
			keys = List.of(new Bucket("signup-ip:" + ip, policy.signupIpBurst(), policy.signupIpRefillSeconds()));
		long retry;
		try {
			retry = store.consume(keys, clock.instant(), policy.maxEntries());
		}
		catch (org.springframework.dao.DataAccessException
				| org.springframework.transaction.TransactionException exception) {
			SecurityProblemWriter.write(response, HttpStatus.SERVICE_UNAVAILABLE,
					"Proteção de autenticação indisponível. Tente novamente.", "AUTH_STORAGE_UNAVAILABLE", mapper);
			return;
		}
		if (retry > 0) {
			response.setHeader("Retry-After", Long.toString(retry));
			response.setHeader("Cache-Control", "no-store");
			SecurityProblemWriter.write(response, HttpStatus.TOO_MANY_REQUESTS,
					"Muitas tentativas. Aguarde antes de tentar novamente.", "RATE_LIMIT_EXCEEDED", mapper);
			return;
		}
		chain.doFilter(request, response);
	}

	private static String path(HttpServletRequest request) {
		String servletPath = request.getServletPath();
		return servletPath.isEmpty() ? request.getRequestURI().substring(request.getContextPath().length())
				: servletPath;
	}

	private static String digest(String value) {
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

}
