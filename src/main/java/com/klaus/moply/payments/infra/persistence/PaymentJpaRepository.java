package com.klaus.moply.payments.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.klaus.moply.payments.domain.Payment;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

	Optional<PaymentEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

	Optional<PaymentEntity> findByOrganizationIdAndWorkOrderIdAndStatus(UUID organizationId, UUID workOrderId,
			Payment.Status status);

	Page<PaymentEntity> findAllByOrganizationIdAndWorkOrderId(UUID organizationId, UUID workOrderId, Pageable pageable);

	List<PaymentEntity> findAllByOrganizationIdAndWorkOrderIdOrderByRecordedAtAsc(UUID organizationId,
			UUID workOrderId);

}
