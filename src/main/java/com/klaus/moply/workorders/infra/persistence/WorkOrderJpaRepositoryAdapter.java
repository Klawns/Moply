package com.klaus.moply.workorders.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.collaborators.application.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderJpaRepositoryAdapter implements WorkOrderRepository, WorkOrderOperations {

	private final WorkOrderJpaRepository repo;

	private final CustomerRepository customers;

	private final CollaboratorRepository collaborators;

	@Override
	@Transactional
	public WorkOrder save(UUID organizationId, WorkOrder work) {
		Objects.requireNonNull(organizationId);
		if (work.id() != null)
			throw new DomainException("Somente criação de trabalhos é suportada.");
		var customer = customers.findById(organizationId, work.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(work.customerId()));
		if (work.customerLocationId() != null)
			customer.findLocation(work.customerLocationId());
		for (var a : work.assignments()) {
			var c = collaborators.findById(organizationId, a.collaboratorId())
				.orElseThrow(() -> new CollaboratorNotFoundException(a.collaboratorId()));
			if (!c.isActive())
				throw new InactiveCollaboratorException();
		}
		return repo.saveAndFlush(WorkOrderEntity.from(organizationId, work)).toDomain();
	}

	@Override
	@Transactional
	public void update(UUID organizationId, UUID id, UnaryOperator<WorkOrder> transition) {
		var entity = repo.findForUpdate(Objects.requireNonNull(organizationId), Objects.requireNonNull(id))
			.orElseThrow(() -> new WorkOrderNotFoundException(id));
		var before = entity.toDomain();
		var after = Objects.requireNonNull(transition.apply(before));
		// Only operational fields are mutable. Never recreate assignments or recalculate
		// history.
		var expected = new WorkOrder(before.id(), before.customerId(), before.customerLocationId(), after.serviceDate(),
				after.startTime(), before.description(), before.contractedHours(), before.hourlyRate(),
				before.currencyCode(), before.totalAmount(), before.allocationPolicyVersion(), after.status(),
				before.version(), before.assignments());
		if (!expected.equals(after))
			throw new IllegalArgumentException("A transição alterou condições imutáveis do trabalho.");
		if (!before.equals(after)) {
			entity.setStatus(after.status());
			entity.setServiceDate(after.serviceDate());
			entity.setStartTime(after.startTime());
			repo.saveAndFlush(entity);
		}
	}

	@Override
	public Optional<WorkOrder> findById(UUID organizationId, UUID id) {
		return repo.findByOrganizationIdAndId(Objects.requireNonNull(organizationId), id)
			.map(WorkOrderEntity::toDomain);
	}

	@Override
	public List<WorkOrder> findAll(UUID organizationId, LocalDate from, LocalDate to, UUID customerId,
			WorkOrderStatus status) {
		Objects.requireNonNull(organizationId);
		return repo.findAllByFilters(organizationId, from, to, customerId, status)
			.stream()
			.map(WorkOrderEntity::toDomain)
			.toList();
	}

}
