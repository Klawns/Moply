package com.klaus.moply.auth.infra.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.klaus.moply.auth.infra.persistence.entities.RevokedTokenEntity;

public interface RevokedTokenJpaRepository extends JpaRepository<RevokedTokenEntity, UUID> {

	@Modifying
	@Query(value = "INSERT INTO tb_revoked_token (id, expires_at) VALUES (:id, :expiresAt) ON CONFLICT (id) DO NOTHING",
			nativeQuery = true)
	void insertIfAbsent(UUID id, Instant expiresAt);

	long deleteByExpiresAtLessThanEqual(Instant now);

}
