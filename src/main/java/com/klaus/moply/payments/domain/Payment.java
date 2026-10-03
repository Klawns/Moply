package com.klaus.moply.payments.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;

public final class Payment {

	public enum Status {

		RECORDED, REVERSED

	}

	private final UUID id;

	private final UUID organizationId;

	private final PaymentAmount amount;

	private final LocalDate paidOn;

	private final Instant recordedAt;

	private final UUID recordedBy;

	private final Status status;

	private final PaymentReversal reversal;

	private Payment(UUID id, UUID organizationId, PaymentAmount amount, LocalDate paidOn, Instant recordedAt,
			UUID recordedBy, Status status, PaymentReversal reversal) {
		if (id == null || organizationId == null || amount == null || paidOn == null || recordedAt == null
				|| recordedBy == null || status == null)
			throw new DomainException("Pagamento inválido.");
		if (status == Status.RECORDED && reversal != null)
			throw new DomainException("Auditoria de reversão inconsistente.");
		if (status == Status.REVERSED && reversal == null)
			throw new DomainException("Reversão exige responsável e motivo.");
		this.id = id;
		this.organizationId = organizationId;
		this.amount = amount;
		this.paidOn = paidOn;
		this.recordedAt = recordedAt;
		this.recordedBy = recordedBy;
		this.status = status;
		this.reversal = reversal;
	}

	public static Payment create(UUID organizationId, PaymentAmount amount, LocalDate paidOn, Instant recordedAt,
			UUID recordedBy) {
		return new Payment(UUID.randomUUID(), organizationId, amount, paidOn, recordedAt, recordedBy, Status.RECORDED,
				null);
	}

	public static Payment restore(UUID id, UUID organizationId, PaymentAmount amount, LocalDate paidOn,
			Instant recordedAt, UUID recordedBy, Status status, PaymentReversal reversal) {
		return new Payment(id, organizationId, amount, paidOn, recordedAt, recordedBy, status, reversal);
	}

	public Payment reverse(UUID actor, String reason, Instant at) {
		if (status == Status.REVERSED)
			throw new DomainException("Pagamento já foi revertido.");
		return new Payment(id, organizationId, amount, paidOn, recordedAt, recordedBy, Status.REVERSED,
				new PaymentReversal(at, actor, reason));
	}

	public UUID id() {
		return id;
	}

	public UUID organizationId() {
		return organizationId;
	}

	public PaymentAmount amount() {
		return amount;
	}

	public LocalDate paidOn() {
		return paidOn;
	}

	public Instant recordedAt() {
		return recordedAt;
	}

	public UUID recordedBy() {
		return recordedBy;
	}

	public Status status() {
		return status;
	}

	public PaymentReversal reversal() {
		return reversal;
	}

}
