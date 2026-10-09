package com.klaus.moply.auth.infra.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.klaus.moply.auth.infra.persistence.entities.AuthRateBucketEntity;

public interface AuthRateBucketJpaRepository extends JpaRepository<AuthRateBucketEntity, String> {

	@Modifying
	@Query("delete from AuthRateBucketEntity b where b.expiresAt <= :now")
	int removeExpired(Instant now);

}
