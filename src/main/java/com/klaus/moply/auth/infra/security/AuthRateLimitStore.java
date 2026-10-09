package com.klaus.moply.auth.infra.security;

import java.time.Instant;
import java.util.List;

/** Authentication traffic is technical infrastructure policy, not business quota. */
public interface AuthRateLimitStore {

	record Bucket(String key, int burst, int refillSeconds) {
	}

	/** Zero permits the request; positive seconds indicate when to try again. */
	long consume(List<Bucket> buckets, Instant now, int maxEntries);

}
