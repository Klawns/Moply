package com.klaus.moply.recurrence.infra.persistence;

import java.util.UUID;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;

public interface RecurrenceSeriesJpaRepository extends JpaRepository<RecurrenceSeriesEntity, UUID> {

	@EntityGraph(attributePaths = "members")
	Optional<RecurrenceSeriesEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from RecurrenceSeriesEntity s where s.organizationId=:account and s.id=:id")
	Optional<RecurrenceSeriesEntity> lock(UUID account, UUID id);

	@Query("select s.id as id, s.organizationId as organizationId from RecurrenceSeriesEntity s where s.id > :after order by s.id")
	List<Reference> nextBatch(UUID after, Pageable pageable);

	@Query("select s.familyId from RecurrenceSeriesEntity s where s.organizationId=:account and s.id=:id")
	Optional<UUID> findFamilyId(UUID account, UUID id);

	@Query("select s.id from RecurrenceSeriesEntity s where s.organizationId=:account and s.familyId=:family order by s.id")
	List<UUID> familyIds(UUID account, UUID family);

	interface Reference {

		UUID getId();

		UUID getOrganizationId();

	}

}
