package com.klaus.moply.recurrence.infra.persistence;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurrenceChangeItemJpaRepository extends JpaRepository<RecurrenceChangeItemEntity, UUID> {

	@Query("select i from RecurrenceChangeItemEntity i, RecurrenceCommandEntity c "
			+ "where i.organizationId=:account and i.workId=:work and c.organizationId=i.organizationId "
			+ "and c.id=i.commandId order by c.recordedAt asc, c.id asc, i.id asc")
	List<RecurrenceChangeItemEntity> findHistoryByRecordedAtAscending(@Param("account") UUID account,
			@Param("work") UUID work, Pageable pageable);

	@Query("select i from RecurrenceChangeItemEntity i, RecurrenceCommandEntity c "
			+ "where i.organizationId=:account and i.workId=:work and c.organizationId=i.organizationId "
			+ "and c.id=i.commandId order by c.recordedAt desc, c.id asc, i.id asc")
	List<RecurrenceChangeItemEntity> findHistoryByRecordedAtDescending(@Param("account") UUID account,
			@Param("work") UUID work, Pageable pageable);

	@Query("select i from RecurrenceChangeItemEntity i, RecurrenceCommandEntity c "
			+ "where i.organizationId=:account and i.workId=:work and c.organizationId=i.organizationId "
			+ "and c.id=i.commandId order by i.id asc")
	List<RecurrenceChangeItemEntity> findHistoryByIdAscending(@Param("account") UUID account, @Param("work") UUID work,
			Pageable pageable);

	@Query("select i from RecurrenceChangeItemEntity i, RecurrenceCommandEntity c "
			+ "where i.organizationId=:account and i.workId=:work and c.organizationId=i.organizationId "
			+ "and c.id=i.commandId order by i.id desc")
	List<RecurrenceChangeItemEntity> findHistoryByIdDescending(@Param("account") UUID account, @Param("work") UUID work,
			Pageable pageable);

	@Query("select count(i) from RecurrenceChangeItemEntity i " + "where i.organizationId=:account and i.workId=:work")
	long countHistory(@Param("account") UUID account, @Param("work") UUID work);

}
