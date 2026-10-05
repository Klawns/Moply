package com.klaus.moply.payments.application.ports;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;

public interface CollaboratorPaymentRepository {

	Payment save(UUID workOrderId, UUID collaboratorId, String idempotencyKey, Payment payment);

	Optional<RecordedPayment> findByIdempotencyKey(UUID organizationId, String idempotencyKey);

	Optional<RecordedPayment> findById(UUID organizationId, UUID paymentId);

	Optional<UUID> findWorkOrderIdByPayment(UUID organizationId, UUID paymentId);

	PageResult<Payment> findAll(UUID organizationId, UUID workOrderId, UUID collaboratorId, PageQuery page);

	List<Payment> findAll(UUID organizationId, UUID workOrderId, UUID collaboratorId);

	List<RecordedTotal> findRecordedTotalsByCollaborator(UUID organizationId, UUID collaboratorId);

	boolean hasRecordedForWork(UUID organizationId, UUID workOrderId);

	void update(Payment payment);

	record RecordedPayment(UUID workOrderId, UUID collaboratorId, Payment payment) {
	}

	record RecordedTotal(UUID workOrderId, BigDecimal amount) {
	}

}
