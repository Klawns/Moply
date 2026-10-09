package com.klaus.moply.payments.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.klaus.moply.payments.application.usecase.dto.RecordWorkOrderPaymentInput;
import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.payments.domain.PaymentAmount;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordWorkOrderPayment implements Usecase.Contextual<RecordWorkOrderPaymentInput, Payment> {

	private final WorkOrderOperations workOrders;

	private final WorkOrderPaymentRepository payments;

	private final OrganizationRepository organizations;

	private final Clock clock;

	@Override
	public Payment execute(Usecase.Context context, RecordWorkOrderPaymentInput input) {
		validateInput(input);

		var organizationId = context.organizationId();

		var organization = organizations.findById(organizationId).orElseThrow(AccountNotFoundException::new);

		var today = LocalDate.now(clock.withZone(ZoneId.of(organization.timezone())));

		return workOrders.withWorkOrder(organizationId, input.workOrderId(),
				work -> recordPayment(organizationId, work, input, today));
	}

	private Payment recordPayment(UUID organizationId, WorkOrder work, RecordWorkOrderPaymentInput input,
			LocalDate today) {

		validateWorkOrder(work, input.paidOn(), today);

		if (payments.findActiveByWork(organizationId, work.id()).isPresent()) {
			throw new PaymentConflictException("O trabalho já possui pagamento ativo.");
		}

		var payment = Payment.create(organizationId, new PaymentAmount(work.totalAmount().value(), work.currencyCode()),
				input.paidOn(), clock.instant(), input.actorId());

		return payments.save(work.id(), payment);
	}

	private void validateInput(RecordWorkOrderPaymentInput input) {
		if (input == null || input.workOrderId() == null || input.paidOn() == null || input.actorId() == null) {
			throw new ApplicationException("Trabalho, data e responsável são obrigatórios.");
		}
	}

	private void validateWorkOrder(WorkOrder work, LocalDate paidOn, LocalDate today) {

		if (work.status() == WorkOrderStatus.CANCELLED) {
			throw new PaymentConflictException("Trabalho cancelado não pode receber pagamento.");
		}

		if (work.serviceDate().isAfter(today)) {
			throw new PaymentConflictException("Trabalho futuro não pode ser pago.");
		}

		if (paidOn.isBefore(work.serviceDate()) || paidOn.isAfter(today)) {
			throw new ApplicationException("Data do pagamento deve estar entre a data do trabalho e hoje.");
		}
	}

}
