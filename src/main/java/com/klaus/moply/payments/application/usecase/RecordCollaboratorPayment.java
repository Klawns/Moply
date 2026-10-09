package com.klaus.moply.payments.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import com.klaus.moply.payments.application.usecase.dto.RecordCollaboratorPaymentInput;
import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.payments.domain.PaymentAmount;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordCollaboratorPayment implements Usecase.Contextual<RecordCollaboratorPaymentInput, Payment> {

	private final WorkOrderOperations workOrders;

	private final CollaboratorPaymentRepository payments;

	private final OrganizationRepository organizations;

	private final Clock clock;

	@Override
	public Payment execute(Usecase.Context context, RecordCollaboratorPaymentInput input) {
		validate(input);
		var organizationId = context.organizationId();
		var organization = organizations.findById(organizationId).orElseThrow(AccountNotFoundException::new);
		var today = LocalDate.now(clock.withZone(ZoneId.of(organization.timezone())));

		return workOrders.withWorkOrder(organizationId, input.workOrderId(), work -> {
			var repeated = payments.findByIdempotencyKey(organizationId, input.idempotencyKey());
			if (repeated.isPresent())
				return existingOrConflict(repeated.get(), input);

			if (work.status() == WorkOrderStatus.CANCELLED)
				throw new PaymentConflictException("Trabalho cancelado não pode receber acertos.");

			var assignment = work.assignments()
				.stream()
				.filter(item -> item.collaboratorId().equals(input.collaboratorId()))
				.findFirst()
				.orElseThrow(() -> new PaymentConflictException("Colaborador não participa deste trabalho."));

			if (work.serviceDate().isAfter(today))
				throw new PaymentConflictException("Trabalho futuro não pode receber acertos.");
			if (input.paidOn().isBefore(work.serviceDate()) || input.paidOn().isAfter(today))
				throw new DomainException("Data do acerto deve estar entre a data do trabalho e hoje.");

			var paid = payments.findRecordedTotalsByCollaborator(organizationId, input.collaboratorId())
				.stream()
				.filter(total -> total.workOrderId().equals(work.id()))
				.map(CollaboratorPaymentRepository.RecordedTotal::amount)
				.findFirst()
				.orElse(BigDecimal.ZERO);
			var remaining = assignment.allocatedAmount().value().subtract(paid);
			if (input.amount().compareTo(remaining) > 0)
				throw new PaymentConflictException("O acerto não pode exceder o saldo restante da participação.");

			var payment = Payment.create(organizationId, new PaymentAmount(input.amount(), work.currencyCode()),
					input.paidOn(), clock.instant(), input.actorId());
			return payments.save(work.id(), input.collaboratorId(), input.idempotencyKey(), payment);
		});
	}

	private Payment existingOrConflict(CollaboratorPaymentRepository.RecordedPayment existing,
			RecordCollaboratorPaymentInput input) {
		if (existing.workOrderId().equals(input.workOrderId())
				&& existing.collaboratorId().equals(input.collaboratorId())
				&& existing.payment().amount().value().compareTo(input.amount()) == 0
				&& existing.payment().paidOn().equals(input.paidOn()))
			return existing.payment();
		throw new PaymentConflictException("A chave de idempotência já foi usada para outro acerto.");
	}

	private void validate(RecordCollaboratorPaymentInput input) {
		if (input == null || input.workOrderId() == null || input.collaboratorId() == null || input.actorId() == null
				|| input.amount() == null || input.paidOn() == null || input.idempotencyKey() == null
				|| input.idempotencyKey().isBlank() || input.idempotencyKey().length() > 128)
			throw new DomainException("Trabalho, colaborador, valor, data, chave e responsável são obrigatórios.");
		if (input.amount().signum() <= 0 || input.amount().scale() > 2)
			throw new DomainException("O valor do acerto deve ser positivo e ter até duas casas decimais.");
	}

}
