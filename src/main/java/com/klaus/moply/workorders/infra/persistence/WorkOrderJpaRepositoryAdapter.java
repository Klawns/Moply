package com.klaus.moply.workorders.infra.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.infra.persistence.PageableMapper;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;

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
			throw new IllegalStateException("Somente criação de trabalhos é suportada.");
		validateCustomer(organizationId, work);
		validateCollaborators(organizationId, work);
		return repo.saveAndFlush(WorkOrderEntity.from(organizationId, work)).toDomain();
	}

	private void validateCustomer(UUID organizationId, WorkOrder work) {
		var customer = customers.findById(organizationId, work.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(work.customerId()));
		if (work.customerLocationId() != null)
			customer.findLocation(work.customerLocationId());
	}

	private void validateCollaborators(UUID organizationId, WorkOrder work) {
		for (var assignment : work.assignments()) {
			var collaborator = collaborators.findById(organizationId, assignment.collaboratorId())
				.orElseThrow(() -> new CollaboratorNotFoundException(assignment.collaboratorId()));
			if (!collaborator.isActive())
				throw new InactiveCollaboratorException();
		}
	}

	@Override
	@Transactional
	public void update(UUID organizationId, UUID id, UnaryOperator<WorkOrder> transition) {
		var entity = lockWorkOrder(organizationId, id);
		var before = entity.toDomain();
		var after = Objects.requireNonNull(transition.apply(before));
		if (!before.hasSameConditionsAs(after)) {
			throw new IllegalArgumentException("A transição alterou condições imutáveis do trabalho.");
		}
		if (!before.hasSameOperationAs(after)) {
			entity.setStatus(after.status());
			entity.setServiceDate(after.serviceDate());
			entity.setStartTime(after.startTime());
			repo.saveAndFlush(entity);
		}
	}

	@Override
	@Transactional
	public <T> T withWorkOrder(UUID organizationId, UUID id, Function<WorkOrder, T> operation) {
		return Objects.requireNonNull(operation).apply(lockWorkOrder(organizationId, id).toDomain());
	}

	private WorkOrderEntity lockWorkOrder(UUID organizationId, UUID id) {
		return repo.findForUpdate(Objects.requireNonNull(organizationId), Objects.requireNonNull(id))
			.orElseThrow(() -> new WorkOrderNotFoundException(id));
	}

	@Override
	public Optional<WorkOrder> findById(UUID organizationId, UUID id) {
		return repo.findByOrganizationIdAndId(Objects.requireNonNull(organizationId), id)
			.map(entity -> Objects.requireNonNull(entity).toDomain());
	}

	@Override
	public List<WorkOrder> findAllByCollaborator(UUID organizationId, UUID collaboratorId) {
		return repo
			.findAllByCollaborator(Objects.requireNonNull(organizationId), Objects.requireNonNull(collaboratorId))
			.stream()
			.map(entity -> Objects.requireNonNull(entity).toDomain())
			.toList();
	}

	@Override
	public List<WorkOrder> findAll(UUID organizationId, WorkOrderDateRange dateRange, UUID customerId,
			WorkOrderStatus status) {
		Objects.requireNonNull(organizationId);
		return repo
			.findAll(WorkOrderSpecifications.filters(organizationId, dateRange, customerId, status),
					Sort.by("serviceDate", "id"))
			.stream()
			.map(entity -> Objects.requireNonNull(entity).toDomain())
			.toList();
	}

	@Override
	public PageResult<WorkOrder> search(UUID organizationId, WorkOrderDateRange dateRange, UUID customerId,
			WorkOrderStatus status, PageQuery page) {
		Objects.requireNonNull(organizationId);
		var specification = WorkOrderSpecifications.filters(organizationId, dateRange, customerId, status);
		var selected = repo.findAll(specification,
				PageableMapper.toPageable(page, Set.of("serviceDate", "startTime", "status", "id"), "serviceDate"));
		return assemblePage(organizationId, selected);
	}

	private PageResult<WorkOrder> assemblePage(UUID organizationId, Page<WorkOrderEntity> selected) {
		if (selected.isEmpty())
			return new PageResult<>(List.of(), selected.getNumber(), selected.getSize(), selected.getTotalElements(),
					selected.getTotalPages());
		var ids = selected.getContent().stream().map(entity -> Objects.requireNonNull(entity).getId()).toList();
		var loadedById = repo.findAllByOrganizationIdAndIdIn(organizationId, ids)
			.stream()
			.collect(Collectors.toMap(entity -> Objects.requireNonNull(entity).getId(), Function.identity()));
		var ordered = ids.stream()
			.map(loadedById::get)
			.map(entity -> Objects.requireNonNull(entity).toDomain())
			.toList();
		return new PageResult<>(ordered, selected.getNumber(), selected.getSize(), selected.getTotalElements(),
				selected.getTotalPages());
	}

}
