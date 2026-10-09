package com.klaus.moply.auth.infra.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.klaus.moply.auth.infra.security.AuthRateLimitStore;
import com.klaus.moply.auth.infra.security.AuthRateLimitStore.Bucket;
import com.klaus.moply.auth.infra.security.TokenRevocations;
import com.klaus.moply.factory.PostgresSpringIntegrationTest;

@SpringBootTest(properties = { "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true" })
@ActiveProfiles("test")
class AuthenticationStorageIntegrationTest extends PostgresSpringIntegrationTest {

	@Autowired
	AuthRateLimitStore store;

	@Autowired
	TokenRevocations revocations;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	com.klaus.moply.auth.infra.persistence.adapters.TokenRevocationsJpaAdapter revocationAdapter;

	private final Instant now = Instant.now().plusSeconds(3600);

	@AfterEach
	void clean() {
		jdbc.update("delete from tb_auth_rate_bucket");
		jdbc.update("delete from tb_revoked_token");
	}

	@Test
	void shouldAllowOnlyBurstUnderConcurrentRequestsAndRefillWithoutExtendingDenial() throws Exception {
		var keys = List.of(new Bucket("concurrent", 5, 60));
		try (var pool = Executors.newFixedThreadPool(8)) {
			Callable<Long> attempt = () -> store.consume(keys, now, 10000);
			var results = pool.invokeAll(Collections.nCopies(16, attempt));
			int permitted = 0;
			for (var result : results) {
				long retry = result.get();
				if (retry == 0)
					permitted++;
				else
					assertEquals(60, retry);
			}
			assertEquals(5, permitted);
		}
		assertEquals(30, store.consume(keys, now.plusSeconds(30), 10000));
		assertEquals(0, store.consume(keys, now.plusSeconds(60), 10000));
		assertEquals(60, store.consume(keys, now.plusSeconds(60), 10000));
	}

	@Test
	void shouldIsolateIdentifiersAndIpPoolsAndDebitAllKeysAtomically() {
		var ip = new Bucket("ip-a", 2, 10);
		var account = new Bucket("account-a", 1, 60);
		assertEquals(0, store.consume(List.of(ip, account), now, 10000));
		assertEquals(60, store.consume(List.of(ip, account), now, 10000));
		assertEquals(0, store.consume(List.of(ip, new Bucket("account-b", 1, 60)), now, 10000));
		assertEquals(10, store.consume(List.of(ip, new Bucket("account-c", 1, 60)), now, 10000));
		assertEquals(0, store.consume(List.of(new Bucket("ip-b", 2, 10), new Bucket("account-c", 1, 60)), now, 10000));
	}

	@Test
	void shouldBoundStateAndRecycleOnlyFullyReplenishedBuckets() {
		for (int i = 0; i < 3; i++)
			assertEquals(0, store.consume(List.of(new Bucket("key-" + i, 1, 60)), now, 3));
		assertEquals(60, store.consume(List.of(new Bucket("overflow", 1, 60)), now, 3));
		assertEquals(3, jdbc.queryForObject("select count(*) from tb_auth_rate_bucket", Integer.class));
		assertEquals(0, store.consume(List.of(new Bucket("overflow", 1, 60)), now.plusSeconds(60), 3));
		assertEquals(1, jdbc.queryForObject("select count(*) from tb_auth_rate_bucket", Integer.class));
	}

	@Test
	void shouldRemoveOnlyExpiredRevocations() {
		var expired = UUID.randomUUID();
		var active = UUID.randomUUID();
		revocations.revoke(expired, Instant.now().minusSeconds(60));
		revocations.revoke(active, now.plusSeconds(1800));
		revocationAdapter.removeExpired();
		assertFalse(revocations.contains(expired));
		assertTrue(revocations.contains(active));
	}

	@Test
	void shouldPersistRevocationsIdempotentlyAndKeepOtherSessionsValid() throws Exception {
		var id = UUID.randomUUID();
		var other = UUID.randomUUID();
		try (var pool = Executors.newFixedThreadPool(4)) {
			Callable<Void> logout = () -> {
				revocations.revoke(id, now.plusSeconds(1800));
				return null;
			};
			for (var result : pool.invokeAll(Collections.nCopies(8, logout)))
				result.get();
		}
		assertTrue(revocations.contains(id));
		assertFalse(revocations.contains(other));
		assertEquals(1, jdbc.queryForObject("select count(*) from tb_revoked_token", Integer.class));
	}

}
