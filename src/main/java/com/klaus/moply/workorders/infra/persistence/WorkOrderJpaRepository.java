package com.klaus.moply.workorders.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import jakarta.persistence.LockModeType;

public interface WorkOrderJpaRepository extends JpaRepository<WorkOrderEntity, UUID> {

	@Query("select w.occurrenceDate from WorkOrderEntity w where w.organizationId = :account and w.recurrenceSeriesId = :series and w.occurrenceDate >= :from and w.occurrenceDate < :until")
	List<LocalDate> findOccurrenceDates(UUID account, UUID series, LocalDate from, LocalDate until);

	// Lock the root only: an outer fetch join of assignments cannot be locked on
	// PostgreSQL.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from WorkOrderEntity w where w.organizationId = :organizationId and w.id = :id")
	Optional<WorkOrderEntity> findForUpdate(UUID organizationId, UUID id);

	@EntityGraph(attributePaths = "assignments")
	Optional<WorkOrderEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

	@EntityGraph(attributePaths = "assignments")
	@Query("""
			select w from WorkOrderEntity w
			where w.organizationId = :organizationId
			 and w.serviceDate >= coalesce(:from, w.serviceDate)
			 and w.serviceDate <= coalesce(:to, w.serviceDate)
			 and w.customerId = coalesce(:customerId, w.customerId)
			 and (:status is null or w.status = :status)
			order by w.serviceDate, w.id
			""")
	List<WorkOrderEntity> findAllByFilters(UUID organizationId, LocalDate from, LocalDate to, UUID customerId,
			WorkOrderStatus status);

	@EntityGraph(attributePaths = "assignments")
	@Query("select distinct w from WorkOrderEntity w join w.assignments a "
			+ "where w.organizationId = :organizationId and a.collaboratorId = :collaboratorId "
			+ "order by w.serviceDate, w.id")
	List<WorkOrderEntity> findAllByCollaborator(UUID organizationId, UUID collaboratorId);

}
