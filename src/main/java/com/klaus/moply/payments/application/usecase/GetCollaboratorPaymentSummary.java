package com.klaus.moply.payments.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Collectors;

import com.klaus.moply.payments.application.usecase.dto.CollaboratorPaymentSummary;
import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetCollaboratorPaymentSummary implements Usecase.Contextual<UUID, CollaboratorPaymentSummary> {

	private final WorkOrderRepository workOrders;

	private final CollaboratorPaymentRepository payments;

	private final CollaboratorRepository collaborators;

	private final OrganizationRepository organizations;

	private final Clock clock;

	@Override
	public CollaboratorPaymentSummary execute(Usecase.Context context, UUID collaboratorId) {
		if (collaboratorId == null)
			throw new DomainException("Colaborador é obrigatório.");
		var organizationId = context.organizationId();
		collaborators.findById(organizationId, collaboratorId)
			.orElseThrow(() -> new CollaboratorNotFoundException(collaboratorId));
		var account = organizations.findById(organizationId).orElseThrow(AccountNotFoundException::new);
		var today = LocalDate.now(clock.withZone(ZoneId.of(account.timezone())));
		var totals = payments.findRecordedTotalsByCollaborator(organizationId, collaboratorId)
			.stream()
			.collect(Collectors.toMap(CollaboratorPaymentRepository.RecordedTotal::workOrderId,
					CollaboratorPaymentRepository.RecordedTotal::amount));
		var balances = workOrders.findAllByCollaborator(organizationId, collaboratorId)
			.stream()
			.filter(work -> work.status() != WorkOrderStatus.CANCELLED)
			.map(work -> balance(work, collaboratorId, totals.getOrDefault(work.id(), BigDecimal.ZERO), today))
			.toList();
		var allocated = balances.stream()
			.map(CollaboratorPaymentSummary.WorkBalance::allocatedAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		var recorded = balances.stream()
			.map(CollaboratorPaymentSummary.WorkBalance::recordedAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		var remaining = allocated.subtract(recorded);
		return new CollaboratorPaymentSummary(collaboratorId, account.currencyCode(), allocated, recorded, remaining,
				balances.stream().anyMatch(CollaboratorPaymentSummary.WorkBalance::requiresAttention), balances);
	}

	private CollaboratorPaymentSummary.WorkBalance balance(WorkOrder work, UUID collaboratorId, BigDecimal recorded,
			LocalDate today) {
		var assignment = work.assignments()
			.stream()
			.filter(item -> item.collaboratorId().equals(collaboratorId))
			.findFirst()
			.orElseThrow();
		var allocated = assignment.allocatedAmount().value();
		var remaining = allocated.subtract(recorded);
		var requiresAttention = work.status() == WorkOrderStatus.COMPLETED && !work.serviceDate().isAfter(today)
				&& remaining.signum() > 0;
		return new CollaboratorPaymentSummary.WorkBalance(work.id(), work.serviceDate(), work.status(), allocated,
				recorded, remaining, requiresAttention);
	}

}
