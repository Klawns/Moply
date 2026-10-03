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
@Table(name = "tb_customer_payment")
@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(name = "work_order_id", nullable = false, updatable = false)
	private UUID workOrderId;

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

	public static PaymentEntity from(UUID workOrderId, Payment payment) {
		var entity = new PaymentEntity();
		var reversal = payment.reversal();
		entity.id = payment.id();
		entity.organizationId = payment.organizationId();
		entity.workOrderId = workOrderId;
		entity.amount = payment.amount().value();
		entity.currencyCode = payment.amount().currencyCode();
		entity.paidOn = payment.paidOn();
		entity.recordedAt = payment.recordedAt();
		entity.recordedBy = payment.recordedBy();
		entity.status = payment.status();
		entity.reversedAt = reversal == null ? null : reversal.at();
		entity.reversedBy = reversal == null ? null : reversal.by();
		entity.reversalReason = reversal == null ? null : reversal.reason();
		return entity;
	}

	public Payment toDomain() {
		var audit = reversedAt == null ? null : new PaymentReversal(reversedAt, reversedBy, reversalReason);
		return Payment.restore(id, organizationId, new PaymentAmount(amount, currencyCode), paidOn, recordedAt,
				recordedBy, status, audit);
	}

}
