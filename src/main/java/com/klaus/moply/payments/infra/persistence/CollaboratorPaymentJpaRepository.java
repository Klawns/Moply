package com.klaus.moply.payments.infra.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.klaus.moply.payments.domain.Payment;

public interface CollaboratorPaymentJpaRepository extends JpaRepository<CollaboratorPaymentEntity, UUID> {

	Optional<CollaboratorPaymentEntity> findByOrganizationIdAndIdempotencyKey(UUID organizationId,
			String idempotencyKey);

	Optional<CollaboratorPaymentEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

	List<CollaboratorPaymentEntity> findAllByOrganizationIdAndWorkOrderIdAndCollaboratorIdOrderByRecordedAtAsc(
			UUID organizationId, UUID workOrderId, UUID collaboratorId);

	@Query("select p.workOrderId as workOrderId, sum(p.amount) as amount from CollaboratorPaymentEntity p "
			+ "where p.organizationId = :organizationId and p.collaboratorId = :collaboratorId "
			+ "and p.status = :status group by p.workOrderId")
	List<RecordedTotal> findRecordedTotals(UUID organizationId, UUID collaboratorId, Payment.Status status);

	boolean existsByOrganizationIdAndWorkOrderIdAndStatus(UUID organizationId, UUID workOrderId, Payment.Status status);

	interface RecordedTotal {

		UUID getWorkOrderId();

		BigDecimal getAmount();

	}

}
