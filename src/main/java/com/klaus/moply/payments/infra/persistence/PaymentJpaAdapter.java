package com.klaus.moply.payments.infra.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.infra.persistence.PageableMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentJpaAdapter implements WorkOrderPaymentRepository {

	private final PaymentJpaRepository repository;

	@Override
	@Transactional
	public Payment save(UUID workOrderId, Payment payment) {
		try {
			return repository.saveAndFlush(PaymentEntity.from(workOrderId, payment)).toDomain();
		}
		catch (DataIntegrityViolationException e) {
			if (e.getMostSpecificCause().getMessage() != null
					&& e.getMostSpecificCause().getMessage().contains("uk_payment_active_work_order"))
				throw new PaymentConflictException("O trabalho já possui pagamento ativo.");
			throw e;
		}
	}

	@Override
	public Optional<Payment> findActiveByWork(UUID organizationId, UUID workOrderId) {
		return repository
			.findByOrganizationIdAndWorkOrderIdAndStatus(organizationId, workOrderId, Payment.Status.RECORDED)
			.map(entity -> Objects.requireNonNull(entity).toDomain());
	}

	@Override
	public Optional<Payment> findById(UUID organizationId, UUID paymentId) {
		return repository.findByOrganizationIdAndId(organizationId, paymentId)
			.map(entity -> Objects.requireNonNull(entity).toDomain());
	}

	@Override
	public Optional<UUID> findWorkOrderIdByPayment(UUID organizationId, UUID paymentId) {
		return repository.findByOrganizationIdAndId(organizationId, paymentId)
			.map(entity -> Objects.requireNonNull(entity).getWorkOrderId());
	}

	@Override
	public PageResult<Payment> findAllByWork(UUID organizationId, UUID workOrderId, PageQuery page) {
		var result = repository.findAllByOrganizationIdAndWorkOrderId(organizationId, workOrderId, PageableMapper
			.toPageable(page, java.util.Set.of("recordedAt", "paidOn", "amount", "status", "id"), "recordedAt"));
		return PageableMapper.toResult(result, entity -> Objects.requireNonNull(entity).toDomain());
	}

	@Override
	public List<Payment> findAllByWork(UUID organizationId, UUID workOrderId) {
		return repository.findAllByOrganizationIdAndWorkOrderIdOrderByRecordedAtAsc(organizationId, workOrderId)
			.stream()
			.map(entity -> Objects.requireNonNull(entity).toDomain())
			.toList();
	}

	@Override
	@Transactional
	public void update(Payment payment) {
		var entity = repository.findByOrganizationIdAndId(payment.organizationId(), payment.id()).orElseThrow();
		var reversal = payment.reversal();
		if (entity.getAmount().compareTo(payment.amount().value()) != 0
				|| !entity.getCurrencyCode().equals(payment.amount().currencyCode())
				|| !entity.getPaidOn().equals(payment.paidOn()) || !entity.getRecordedAt().equals(payment.recordedAt())
				|| !entity.getRecordedBy().equals(payment.recordedBy()))
			throw new IllegalArgumentException("A reversão não pode alterar o registro original.");
		entity.setStatus(payment.status());
		entity.setReversedAt(reversal == null ? null : reversal.at());
		entity.setReversedBy(reversal == null ? null : reversal.by());
		entity.setReversalReason(reversal == null ? null : reversal.reason());
		repository.saveAndFlush(entity);
	}

}
