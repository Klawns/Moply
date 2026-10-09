package com.klaus.moply.auth.infra.persistence.adapters;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.klaus.moply.auth.infra.persistence.AuthRateBucketJpaRepository;
import com.klaus.moply.auth.infra.persistence.AuthRateGuardJpaRepository;
import com.klaus.moply.auth.infra.persistence.entities.AuthRateBucketEntity;
import com.klaus.moply.auth.infra.security.AuthRateLimitStore;

@Component
public class AuthRateLimitJpaAdapter implements AuthRateLimitStore {

	private final AuthRateBucketJpaRepository buckets;

	private final AuthRateGuardJpaRepository guard;

	private final Clock clock;

	public AuthRateLimitJpaAdapter(AuthRateBucketJpaRepository buckets, AuthRateGuardJpaRepository guard, Clock clock) {
		this.buckets = buckets;
		this.guard = guard;
		this.clock = clock;
	}

	@Override
	@Transactional(timeout = 5)
	public long consume(List<Bucket> policies, Instant now, int maxEntries) {
		final Instant timestamp = now.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
		// Shared row lock makes creation, capacity checks and multi-key debit atomic
		// across replicas.
		guard.lockGuard().orElseThrow();
		buckets.removeExpired(timestamp);
		List<AuthRateBucketEntity> states = new ArrayList<>();
		int missing = 0;
		long retry = 0;
		for (var policy : policies) {
			var existing = buckets.findById(policy.key());
			if (existing.isEmpty())
				missing++;
			var state = existing.orElseGet(() -> new AuthRateBucketEntity(policy, timestamp));
			states.add(state);
			retry = Math.max(retry, state.retryAfter(policy, timestamp));
		}
		if (retry > 0)
			return retry;
		if (missing > 0 && buckets.count() + missing > maxEntries)
			return 60;
		for (int i = 0; i < policies.size(); i++)
			states.get(i).consume(policies.get(i), timestamp);
		buckets.saveAllAndFlush(states);
		return 0;
	}

	@Scheduled(fixedDelay = 60000, initialDelay = 60000)
	@Transactional(timeout = 5)
	public void removeExpired() {
		guard.lockGuard().orElseThrow();
		buckets.removeExpired(clock.instant());
	}

}
