package com.klaus.moply.auth.infra.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import com.klaus.moply.auth.infra.persistence.entities.AuthRateGuardEntity;

public interface AuthRateGuardJpaRepository extends JpaRepository<AuthRateGuardEntity, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select g from AuthRateGuardEntity g where g.id = 1")
	Optional<AuthRateGuardEntity> lockGuard();

}
