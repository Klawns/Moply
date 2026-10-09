package com.klaus.moply.auth.infra.persistence.entities;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.klaus.moply.auth.infra.security.AuthRateLimitStore.Bucket;

@Entity
@Table(name = "tb_auth_rate_bucket")
@Getter
@NoArgsConstructor
public class AuthRateBucketEntity {

	@Id
	@Column(length = 80)
	private String id;

	@Column(nullable = false)
	private double tokens;

	@Column(nullable = false)
	private Instant updatedAt;

	@Column(nullable = false)
	private Instant expiresAt;

	public AuthRateBucketEntity(Bucket policy, Instant now) {
		id = policy.key();
		tokens = policy.burst();
		updatedAt = now;
		expiresAt = now;
	}

	public double available(Bucket policy, Instant now) {
		double elapsed = Math.max(0, java.time.Duration.between(updatedAt, now).toMillis()) / 1000.0;
		return Math.min(policy.burst(), tokens + elapsed / policy.refillSeconds());
	}

	public long retryAfter(Bucket policy, Instant now) {
		return Math.max(0, (long) Math.ceil((1 - available(policy, now)) * policy.refillSeconds()));
	}

	public void consume(Bucket policy, Instant now) {
		tokens = available(policy, now) - 1;
		updatedAt = now.isBefore(updatedAt) ? updatedAt : now;
		expiresAt = updatedAt.plusMillis((long) Math.ceil((policy.burst() - tokens) * policy.refillSeconds() * 1000));
	}

}
