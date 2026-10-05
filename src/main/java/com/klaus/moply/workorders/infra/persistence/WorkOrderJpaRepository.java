package com.klaus.moply.workorders.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface WorkOrderJpaRepository
		extends JpaRepository<WorkOrderEntity, UUID>, JpaSpecificationExecutor<WorkOrderEntity> {

	/**
	 * Query projection of immutable occurrence identity; avoids loading assignments and
	 * operational state before locking.
	 */
	interface OccurrenceReference {

		UUID getId();

		UUID getSeriesId();

		LocalDate getOriginalDate();

	}

	@Query("""
			select w.id as id, w.recurrenceSeriesId as seriesId, w.occurrenceDate as originalDate
			from WorkOrderEntity w
			where w.organizationId = :organizationId
			 and w.id = :id
			""")
	Optional<OccurrenceReference> occurrenceReference(UUID organizationId, UUID id);

	@Query("""
			select w.id as id, w.recurrenceSeriesId as seriesId, w.occurrenceDate as originalDate
			from WorkOrderEntity w
			where w.organizationId = :organizationId
			 and w.recurrenceSeriesId = :seriesId
			""")
	List<OccurrenceReference> seriesOccurrences(UUID organizationId, UUID seriesId);

	@Query("""
			select w.occurrenceDate
			from WorkOrderEntity w
			where w.organizationId = :organizationId
			 and w.recurrenceSeriesId = :seriesId
			 and w.occurrenceDate >= :from
			 and w.occurrenceDate < :until
			""")
	List<LocalDate> findOccurrenceDates(UUID organizationId, UUID seriesId, LocalDate from, LocalDate until);

	// Lock the root only: an outer fetch join of assignments cannot be locked on
	// PostgreSQL.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select w
			from WorkOrderEntity w
			where w.organizationId = :organizationId
			 and w.id = :id
			""")
	Optional<WorkOrderEntity> findForUpdate(UUID organizationId, UUID id);

	@EntityGraph(attributePaths = "assignments")
	Optional<WorkOrderEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

	@EntityGraph(attributePaths = "assignments")
	List<WorkOrderEntity> findAllByOrganizationIdAndIdIn(UUID organizationId, List<UUID> ids);

	@Override
	@EntityGraph(attributePaths = "assignments")
	List<WorkOrderEntity> findAll(Specification<WorkOrderEntity> specification, Sort sort);

	@EntityGraph(attributePaths = "assignments")
	@Query("""
			select distinct w from WorkOrderEntity w join w.assignments a
			where w.organizationId = :organizationId
			 and a.collaboratorId = :collaboratorId
			order by w.serviceDate, w.id
			""")
	List<WorkOrderEntity> findAllByCollaborator(UUID organizationId, UUID collaboratorId);

}
