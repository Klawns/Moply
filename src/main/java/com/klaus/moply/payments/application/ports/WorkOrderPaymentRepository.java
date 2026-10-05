package com.klaus.moply.payments.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;

public interface WorkOrderPaymentRepository {

	Payment save(UUID workOrderId, Payment payment);

	Optional<Payment> findActiveByWork(UUID organizationId, UUID workOrderId);

	Optional<Payment> findById(UUID organizationId, UUID paymentId);

	Optional<UUID> findWorkOrderIdByPayment(UUID organizationId, UUID paymentId);

	PageResult<Payment> findAllByWork(UUID organizationId, UUID workOrderId, PageQuery page);

	List<Payment> findAllByWork(UUID organizationId, UUID workOrderId);

	void update(Payment payment);

}
