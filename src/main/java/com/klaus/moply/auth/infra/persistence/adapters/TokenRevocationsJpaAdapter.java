package com.klaus.moply.auth.infra.persistence.adapters;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.klaus.moply.auth.infra.persistence.RevokedTokenJpaRepository;
import com.klaus.moply.auth.infra.security.TokenRevocations;

@Component
public class TokenRevocationsJpaAdapter implements TokenRevocations {

	private final RevokedTokenJpaRepository tokens;

	private final Clock clock;

	public TokenRevocationsJpaAdapter(RevokedTokenJpaRepository tokens, Clock clock) {
		this.tokens = tokens;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean contains(UUID id) {
		return tokens.existsById(id);
	}

	@Override
	@Transactional
	public void revoke(UUID id, Instant expiresAt) {
		tokens.insertIfAbsent(id, expiresAt);
	}

	@Scheduled(fixedDelay = 60000, initialDelay = 60000)
	@Transactional
	public void removeExpired() {
		tokens.deleteByExpiresAtLessThanEqual(clock.instant());
	}

}
