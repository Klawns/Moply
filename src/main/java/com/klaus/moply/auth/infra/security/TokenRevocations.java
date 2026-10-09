package com.klaus.moply.auth.infra.security;

import java.time.Instant;
import java.util.UUID;

/** Technical authentication storage contract; no framework types. */
public interface TokenRevocations {

	boolean contains(UUID tokenId);

	void revoke(UUID tokenId, Instant expiresAt);

}
