package com.klaus.moply.workorders.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RescheduleWorkOrder implements Usecase.Contextual<RescheduleWorkOrder.Input, Void> {

	private final WorkOrderOperations operations;

	private final OrganizationRepository accounts;

	private final Clock clock;

	public record Input(UUID id, LocalDate serviceDate, LocalTime startTime) {
	}

	@Override
	public Void execute(Usecase.Context context, Input input) {
		if (input == null || input.id() == null || input.serviceDate() == null)
			throw new DomainException("Trabalho e data obrigatórios.");
		operations.update(context.organizationId(), input.id(), work -> {
			var account = accounts.findById(context.organizationId()).orElseThrow(AccountNotFoundException::new);
			var today = LocalDate.now(clock.withZone(ZoneId.of(account.timezone())));
			return work.reschedule(input.serviceDate(), input.startTime(), today);
		});
		return null;
	}

}
