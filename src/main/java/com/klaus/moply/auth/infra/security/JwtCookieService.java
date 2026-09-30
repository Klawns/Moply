package com.klaus.moply.auth.infra.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import com.klaus.moply.accounts.application.ports.AppUserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Component
public class JwtCookieService {

	public static final String COOKIE = "MOPLY_AUTH";

	private final JwtEncoder encoder;

	private final JwtDecoder decoder;

	private final Clock clock;

	private final AppUserRepository users;

	private final boolean secure;

	private final String issuer;

	private final String audience;

	public JwtCookieService(@Value("${moply.auth.jwt.secret}") String secret,
			@Value("${moply.auth.cookie.secure:false}") boolean secure,
			@Value("${moply.auth.jwt.issuer:moply}") String issuer,
			@Value("${moply.auth.jwt.audience:moply-api}") String audience, Clock clock, AppUserRepository users) {
		byte[] bytes = Base64.getDecoder().decode(secret);
		if (bytes.length < 32)
			throw new IllegalArgumentException("JWT secret must contain at least 256 bits");
		var key = new SecretKeySpec(bytes, "HmacSHA256");
		encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		var timestamps = new JwtTimestampValidator(Duration.ZERO);
		timestamps.setClock(clock);
		decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
				timestamps, new JwtIssuerValidator(issuer)));
		this.decoder = decoder;
		this.clock = clock;
		this.users = users;
		this.secure = secure;
		this.issuer = issuer;
		this.audience = audience;
	}

	public String issue(AccountPrincipal principal) {
		var now = clock.instant();
		var claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.audience(List.of(audience))
			.subject(principal.getUserId().toString())
			.claim("organization_id", principal.getOrganizationId().toString())
			.issuedAt(now)
			.expiresAt(now.plusSeconds(1800))
			.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	public AccountPrincipal authenticate(String token) {
		var jwt = decoder.decode(token);
		if (jwt.getExpiresAt() == null || jwt.getIssuedAt() == null || jwt.getIssuedAt().isAfter(clock.instant())
				|| !jwt.getExpiresAt().isAfter(clock.instant())
				|| jwt.getExpiresAt().isAfter(jwt.getIssuedAt().plusSeconds(1800))
				|| !jwt.getExpiresAt().isAfter(jwt.getIssuedAt()) || jwt.getAudience() == null
				|| !jwt.getAudience().contains(audience))
			throw new BadJwtException("Invalid claims");
		if (jwt.getSubject() == null || jwt.getClaimAsString("organization_id") == null) {
			throw new BadJwtException("Missing identity");
		}
		var user = users.findById(UUID.fromString(jwt.getSubject()))
			.orElseThrow(() -> new BadJwtException("Invalid user"));
		if (!user.getOrganizationId().equals(UUID.fromString(jwt.getClaimAsString("organization_id"))))
			throw new BadJwtException("Invalid account");
		return new AccountPrincipal(user);
	}

	public ResponseCookie cookie(String value) {
		return ResponseCookie.from(COOKIE, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite("Lax")
			.path("/api/v1")
			.maxAge(value.isEmpty() ? Duration.ZERO : Duration.ofMinutes(30))
			.build();
	}

}
