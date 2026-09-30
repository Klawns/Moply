package com.klaus.moply.workorders.application.usecase;

import java.util.UUID;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.collaborators.application.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateWorkOrder implements Usecase.Contextual<CreateWorkOrderInput, WorkOrderOutput> {

	private final WorkOrderRepository repo;

	private final CustomerRepository customers;

	private final CollaboratorRepository collaborators;

	private final OrganizationRepository accounts;

	@Override
	public WorkOrderOutput execute(Usecase.Context context, CreateWorkOrderInput input) {
		validateInput(input);

		var organizationId = context.organizationId();

		var organization = findOrganization(organizationId);
		var status = resolveInitialStatus(input, organization);

		var workOrder = createWorkOrder(input, status);
		var customer = findCustomer(organizationId, input.customerId());

		validateCustomerLocation(customer, input.customerLocationId());
		validateCollaborators(organizationId, workOrder);

		var savedWorkOrder = repo.save(organizationId, workOrder);

		return WorkOrderOutput.from(savedWorkOrder, customer.getName().value());
	}

	private void validateInput(CreateWorkOrderInput input) {
		if (input == null || input.customerId() == null) {
			throw new DomainException("Cliente obrigatório.");
		}
	}

	private Organization findOrganization(UUID organizationId) {
		return accounts.findById(organizationId).orElseThrow(AccountNotFoundException::new);
	}

	private WorkOrderStatus resolveInitialStatus(CreateWorkOrderInput input, Organization organization) {
		if (input.initialStatus() != null) {
			return input.initialStatus();
		}

		return WorkOrderStatus.valueOf(organization.defaultWorkStatus().name());
	}

	private WorkOrder createWorkOrder(CreateWorkOrderInput input, WorkOrderStatus status) {
		return WorkOrder.create(input.customerId(), input.customerLocationId(), input.serviceDate(), input.startTime(),
				input.description(), input.contractedHours(), input.hourlyRate(), input.participantIds(), status);
	}

	private Customer findCustomer(UUID organizationId, UUID customerId) {
		return customers.findById(organizationId, customerId)
			.orElseThrow(() -> new CustomerNotFoundException(customerId));
	}

	private void validateCustomerLocation(Customer customer, UUID customerLocationId) {
		if (customerLocationId == null) {
			return;
		}

		customer.findLocation(customerLocationId);
	}

	private void validateCollaborators(UUID organizationId, WorkOrder workOrder) {
		for (var assignment : workOrder.assignments()) {
			validateCollaborator(organizationId, assignment.collaboratorId());
		}
	}

	private void validateCollaborator(UUID organizationId, UUID collaboratorId) {
		var collaborator = collaborators.findById(organizationId, collaboratorId)
			.orElseThrow(() -> new CollaboratorNotFoundException(collaboratorId));

		if (!collaborator.isActive()) {
			throw new InactiveCollaboratorException();
		}
	}

}
