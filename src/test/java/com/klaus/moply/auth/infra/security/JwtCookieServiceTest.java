package com.klaus.moply.auth.infra.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.klaus.moply.accounts.application.ports.AppUserRepository;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.LoginEmail;

class JwtCookieServiceTest {

	private static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

	private final TokenRevocations revocations = mock(TokenRevocations.class);

	private final AppUserRepository users = mock(AppUserRepository.class);

	private final AppUser user = new AppUser(UUID.randomUUID(), UUID.randomUUID(), new LoginEmail("test@example.com"),
			"encoded");

	private final Instant now = Instant.parse("2026-09-28T12:00:00Z");

	private JwtCookieService service(Instant time, String key) {
		return new JwtCookieService(key, true, "moply", "moply-api", Clock.fixed(time, ZoneOffset.UTC), users,
				revocations);
	}

	@Test
	void shouldExpireAtThirtyMinutesWithoutSlidingRenewal() {
		when(users.findById(user.getId())).thenReturn(Optional.of(user));
		String token = service(now, KEY).issue(new AccountPrincipal(user));
		assertEquals(user.getOrganizationId(),
				service(now.plusSeconds(1799), KEY).authenticate(token).getOrganizationId());
		assertThrows(JwtException.class, () -> service(now.plusSeconds(1800), KEY).authenticate(token));
		assertThrows(JwtException.class, () -> service(now.plusSeconds(1801), KEY).authenticate(token));
	}

	@Test
	void shouldRejectWrongSignatureIssuerAudienceAndUserAccountBinding() {
		when(users.findById(user.getId())).thenReturn(Optional.of(user));
		String token = service(now, KEY).issue(new AccountPrincipal(user));
		byte[] otherBytes = new byte[32];
		Arrays.fill(otherBytes, (byte) 1);
		String otherKey = Base64.getEncoder().encodeToString(otherBytes);
		assertThrows(JwtException.class, () -> service(now, otherKey).authenticate(token));
		var wrongIssuer = new JwtCookieService(KEY, true, "other", "moply-api", Clock.fixed(now, ZoneOffset.UTC), users,
				revocations);
		assertThrows(JwtException.class, () -> wrongIssuer.authenticate(token));
		var wrongAudience = new JwtCookieService(KEY, true, "moply", "other", Clock.fixed(now, ZoneOffset.UTC), users,
				revocations);
		assertThrows(JwtException.class, () -> wrongAudience.authenticate(token));
		when(users.findById(user.getId()))
			.thenReturn(Optional.of(new AppUser(user.getId(), UUID.randomUUID(), user.getEmail(), "encoded")));
		assertThrows(JwtException.class, () -> service(now, KEY).authenticate(token));
		when(users.findById(user.getId())).thenReturn(Optional.empty());
		assertThrows(JwtException.class, () -> service(now, KEY).authenticate(token));
	}

	@Test
	void shouldRejectMissingAndInvalidRequiredClaims() {
		var key = new javax.crypto.spec.SecretKeySpec(Base64.getDecoder().decode(KEY), "HmacSHA256");
		var encoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
		for (String missing : List.of("sub", "organization_id", "exp", "iat", "aud", "jti")) {
			var builder = JwtClaimsSet.builder().issuer("moply");
			if (!missing.equals("jti"))
				builder.id(UUID.randomUUID().toString());
			if (!missing.equals("sub"))
				builder.subject(user.getId().toString());
			if (!missing.equals("organization_id"))
				builder.claim("organization_id", user.getOrganizationId().toString());
			if (!missing.equals("exp"))
				builder.expiresAt(now.plusSeconds(1800));
			if (!missing.equals("iat"))
				builder.issuedAt(now);
			if (!missing.equals("aud"))
				builder.audience(List.of("moply-api"));
			var token = encoder.encode(JwtEncoderParameters.from(
					JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),
					builder.build()))
				.getTokenValue();
			assertThrows(JwtException.class, () -> service(now, KEY).authenticate(token), missing);
		}
	}

	@Test
	void shouldRevokeOnlyTheLoggedOutToken() {
		when(users.findById(user.getId())).thenReturn(Optional.of(user));
		var ids = new java.util.HashSet<UUID>();
		org.mockito.Mockito.doAnswer(call -> {
			ids.add(call.getArgument(0));
			return null;
		}).when(revocations).revoke(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
		when(revocations.contains(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(call -> ids.contains(call.getArgument(0)));
		var service = service(now, KEY);
		String first = service.issue(new AccountPrincipal(user));
		String second = service.issue(new AccountPrincipal(user));
		assertEquals(user.getId(), service.authenticate(first).getUserId());
		service.revoke(first);
		service.revoke(first);
		assertThrows(JwtException.class, () -> service.authenticate(first));
		assertEquals(user.getId(), service.authenticate(second).getUserId());
		service.revoke("invalid");
	}

	@Test
	void shouldRequireStrongKeyAndSecureHostOnlyApiCookie() {
		assertThrows(IllegalArgumentException.class,
				() -> service(now, Base64.getEncoder().encodeToString(new byte[16])));
		var cookie = service(now, KEY).cookie("example");
		assertTrue(cookie.isSecure());
		assertTrue(cookie.isHttpOnly());
		assertNull(cookie.getDomain());
		assertEquals("Lax", cookie.getSameSite());
		assertEquals("/api/v1", cookie.getPath());
		assertEquals(Duration.ofMinutes(30), cookie.getMaxAge());
		assertEquals(Duration.ZERO, service(now, KEY).cookie("").getMaxAge());
	}

}
