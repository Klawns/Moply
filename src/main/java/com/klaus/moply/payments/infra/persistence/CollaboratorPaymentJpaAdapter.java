package com.klaus.moply.payments.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.infra.persistence.PageableMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollaboratorPaymentJpaAdapter implements CollaboratorPaymentRepository {

	private final CollaboratorPaymentJpaRepository repository;

	@Override
	@Transactional
	public Payment save(UUID workOrderId, UUID collaboratorId, String idempotencyKey, Payment payment) {
		try {
			return repository
				.saveAndFlush(CollaboratorPaymentEntity.from(workOrderId, collaboratorId, idempotencyKey, payment))
				.toDomain();
		}
		catch (DataIntegrityViolationException exception) {
			var cause = exception.getMostSpecificCause();
			if (cause.getMessage() != null && cause.getMessage().contains("uk_collaborator_payment_idempotency"))
				throw new PaymentConflictException("A chave de idempotência já foi utilizada.");
			throw exception;
		}
	}

	@Override
	public Optional<RecordedPayment> findByIdempotencyKey(UUID organizationId, String idempotencyKey) {
		return repository.findByOrganizationIdAndIdempotencyKey(organizationId, idempotencyKey)
			.map(this::toRecordedPayment);
	}

	@Override
	public Optional<RecordedPayment> findById(UUID organizationId, UUID paymentId) {
		return repository.findByOrganizationIdAndId(organizationId, paymentId).map(this::toRecordedPayment);
	}

	@Override
	public Optional<UUID> findWorkOrderIdByPayment(UUID organizationId, UUID paymentId) {
		return repository.findByOrganizationIdAndId(organizationId, paymentId)
			.map(CollaboratorPaymentEntity::getWorkOrderId);
	}

	@Override
	public PageResult<Payment> findAll(UUID organizationId, UUID workOrderId, UUID collaboratorId, PageQuery page) {
		var result = repository.findAllByOrganizationIdAndWorkOrderIdAndCollaboratorId(organizationId, workOrderId,
				collaboratorId, PageableMapper.toPageable(page,
						java.util.Set.of("recordedAt", "paidOn", "amount", "status", "id"), "recordedAt"));
		return PageableMapper.toResult(result, CollaboratorPaymentEntity::toDomain);
	}

	@Override
	public List<Payment> findAll(UUID organizationId, UUID workOrderId, UUID collaboratorId) {
		return repository
			.findAllByOrganizationIdAndWorkOrderIdAndCollaboratorIdOrderByRecordedAtAsc(organizationId, workOrderId,
					collaboratorId)
			.stream()
			.map(CollaboratorPaymentEntity::toDomain)
			.toList();
	}

	@Override
	public List<RecordedTotal> findRecordedTotalsByCollaborator(UUID organizationId, UUID collaboratorId) {
		return repository.findRecordedTotals(organizationId, collaboratorId, Payment.Status.RECORDED)
			.stream()
			.map(total -> new RecordedTotal(total.getWorkOrderId(), total.getAmount()))
			.toList();
	}

	@Override
	public boolean hasRecordedForWork(UUID organizationId, UUID workOrderId) {
		return repository.existsByOrganizationIdAndWorkOrderIdAndStatus(organizationId, workOrderId,
				Payment.Status.RECORDED);
	}

	@Override
	@Transactional
	public void update(Payment payment) {
		var entity = repository.findByOrganizationIdAndId(payment.organizationId(), payment.id()).orElseThrow();
		if (entity.getAmount().compareTo(payment.amount().value()) != 0
				|| !entity.getCurrencyCode().equals(payment.amount().currencyCode())
				|| !entity.getPaidOn().equals(payment.paidOn()) || !entity.getRecordedAt().equals(payment.recordedAt())
				|| !entity.getRecordedBy().equals(payment.recordedBy()))
			throw new IllegalArgumentException("A reversão não pode alterar o registro original.");
		entity.setStatus(payment.status());
		entity.setReversedAt(payment.reversal() == null ? null : payment.reversal().at());
		entity.setReversedBy(payment.reversal() == null ? null : payment.reversal().by());
		entity.setReversalReason(payment.reversal() == null ? null : payment.reversal().reason());
		repository.saveAndFlush(entity);
	}

	private RecordedPayment toRecordedPayment(CollaboratorPaymentEntity entity) {
		return new RecordedPayment(entity.getWorkOrderId(), entity.getCollaboratorId(), entity.toDomain());
	}

}
