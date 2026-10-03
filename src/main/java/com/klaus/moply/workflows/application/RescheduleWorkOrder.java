package com.klaus.moply.workflows.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RescheduleWorkOrder implements Usecase.Contextual<RescheduleWorkOrder.Input, Void> {

	private final WorkOrderOperations operations;

	private final WorkOrderPaymentRepository payments;

	private final OrganizationRepository organizations;

	private final Clock clock;

	public record Input(UUID id, LocalDate serviceDate, LocalTime startTime) {
	}

	@Override
	public Void execute(Usecase.Context context, Input input) {
		if (input == null || input.id() == null || input.serviceDate() == null)
			throw new com.klaus.moply.shared.domain.exception.DomainException("Trabalho e data obrigatórios.");
		var account = organizations.findById(context.organizationId()).orElseThrow(AccountNotFoundException::new);
		var today = LocalDate.now(clock.withZone(ZoneId.of(account.timezone())));
		operations.update(context.organizationId(), input.id(), work -> {
			if (payments.findActiveByWork(context.organizationId(), work.id()).isPresent())
				throw new com.klaus.moply.payments.application.usecase.exception.PaymentConflictException(
						"Trabalho pago não pode ser reagendado.");
			return work.reschedule(input.serviceDate(), input.startTime(), today);
		});
		return null;
	}

}
