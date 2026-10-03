package com.klaus.moply.payments.infra.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.payments.domain.PaymentAmount;
import com.klaus.moply.payments.domain.PaymentReversal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_collaborator_payment")
@Getter
@Setter
@NoArgsConstructor
public class CollaboratorPaymentEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(name = "work_order_id", nullable = false, updatable = false)
	private UUID workOrderId;

	@Column(name = "collaborator_id", nullable = false, updatable = false)
	private UUID collaboratorId;

	@Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
	private String idempotencyKey;

	@Column(nullable = false, columnDefinition = "numeric", updatable = false)
	private BigDecimal amount;

	@Column(name = "currency_code", nullable = false, length = 3, updatable = false)
	private String currencyCode;

	@Column(name = "paid_on", nullable = false, updatable = false)
	private LocalDate paidOn;

	@Column(name = "recorded_at", nullable = false, updatable = false)
	private Instant recordedAt;

	@Column(name = "recorded_by", nullable = false, updatable = false)
	private UUID recordedBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Payment.Status status;

	@Column(name = "reversed_at")
	private Instant reversedAt;

	@Column(name = "reversed_by")
	private UUID reversedBy;

	@Column(name = "reversal_reason", columnDefinition = "text")
	private String reversalReason;

	static CollaboratorPaymentEntity from(UUID workOrderId, UUID collaboratorId, String idempotencyKey,
			Payment payment) {
		var entity = new CollaboratorPaymentEntity();
		entity.id = payment.id();
		entity.organizationId = payment.organizationId();
		entity.workOrderId = workOrderId;
		entity.collaboratorId = collaboratorId;
		entity.idempotencyKey = idempotencyKey;
		entity.amount = payment.amount().value();
		entity.currencyCode = payment.amount().currencyCode();
		entity.paidOn = payment.paidOn();
		entity.recordedAt = payment.recordedAt();
		entity.recordedBy = payment.recordedBy();
		entity.status = payment.status();
		if (payment.reversal() != null) {
			entity.reversedAt = payment.reversal().at();
			entity.reversedBy = payment.reversal().by();
			entity.reversalReason = payment.reversal().reason();
		}
		return entity;
	}

	Payment toDomain() {
		var reversal = reversedAt == null ? null : new PaymentReversal(reversedAt, reversedBy, reversalReason);
		return Payment.restore(id, organizationId, new PaymentAmount(amount, currencyCode), paidOn, recordedAt,
				recordedBy, status, reversal);
	}

}
