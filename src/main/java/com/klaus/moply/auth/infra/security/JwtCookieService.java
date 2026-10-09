package com.klaus.moply.auth.infra.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.klaus.moply.accounts.application.ports.AppUserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Service
public class JwtCookieService {

	public static final String COOKIE = "MOPLY_AUTH";

	private static final String ORGANIZATION_ID_CLAIM = "organization_id";

	private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

	private final JwtEncoder encoder;

	private final JwtDecoder decoder;

	private final Clock clock;

	private final AppUserRepository users;

	private final TokenRevocations revocations;

	private final boolean secure;

	private final String issuer;

	private final String audience;

	public JwtCookieService(@Value("${moply.auth.jwt.secret}") String secret,
			@Value("${moply.auth.cookie.secure:false}") boolean secure,
			@Value("${moply.auth.jwt.issuer:moply}") String issuer,
			@Value("${moply.auth.jwt.audience:moply-api}") String audience, Clock clock, AppUserRepository users,
			TokenRevocations revocations) {
		byte[] bytes = Base64.getDecoder().decode(secret);
		if (bytes.length < 32) {
			throw new IllegalArgumentException("JWT secret must contain at least 256 bits");
		}
		var key = new SecretKeySpec(bytes, "HmacSHA256");
		encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		var timestamps = new JwtTimestampValidator(Duration.ZERO);
		timestamps.setClock(clock);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(issuer)));
		this.decoder = decoder;
		this.clock = clock;
		this.users = users;
		this.revocations = revocations;
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
			.id(UUID.randomUUID().toString())
			.claim(ORGANIZATION_ID_CLAIM, principal.getOrganizationId().toString())
			.issuedAt(now)
			.expiresAt(now.plus(TOKEN_LIFETIME))
			.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	public AccountPrincipal authenticate(String token) {
		var jwt = decoder.decode(token);
		validateClaims(jwt);
		if (revocations.contains(tokenId(jwt)))
			throw new BadJwtException("Revoked token");

		var subject = jwt.getSubject();
		var organizationId = jwt.getClaimAsString(ORGANIZATION_ID_CLAIM);
		if (subject == null || organizationId == null) {
			throw new BadJwtException("Missing identity");
		}
		var user = users.findById(UUID.fromString(subject)).orElseThrow(() -> new BadJwtException("Invalid user"));
		if (!user.getOrganizationId().equals(UUID.fromString(organizationId))) {
			throw new BadJwtException("Invalid account");
		}
		return new AccountPrincipal(user);
	}

	public void revoke(String token) {
		Jwt jwt;
		UUID id;
		try {
			jwt = decoder.decode(token);
			validateClaims(jwt);
			id = tokenId(jwt);
		}
		catch (org.springframework.security.oauth2.jwt.JwtException | IllegalArgumentException exception) {
			return;
		}
		revocations.revoke(id, jwt.getExpiresAt());
	}

	private UUID tokenId(Jwt jwt) {
		if (jwt.getId() == null)
			throw new BadJwtException("Missing token identity");
		return UUID.fromString(jwt.getId());
	}

	private void validateClaims(Jwt jwt) {
		var expiresAt = jwt.getExpiresAt();
		var issuedAt = jwt.getIssuedAt();
		var audiences = jwt.getAudience();
		if (expiresAt == null || issuedAt == null || audiences == null) {
			throw new BadJwtException("Invalid claims");
		}

		var now = clock.instant();
		if (issuedAt.isAfter(now) || !expiresAt.isAfter(now) || expiresAt.isAfter(issuedAt.plus(TOKEN_LIFETIME))
				|| !expiresAt.isAfter(issuedAt) || !audiences.contains(audience)) {
			throw new BadJwtException("Invalid claims");
		}
	}

	public ResponseCookie cookie(String value) {
		return ResponseCookie.from(COOKIE, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite("Lax")
			.path("/api/v1")
			.maxAge(value.isEmpty() ? Duration.ZERO : TOKEN_LIFETIME)
			.build();
	}

}
