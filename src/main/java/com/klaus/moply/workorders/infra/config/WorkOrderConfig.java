package com.klaus.moply.workorders.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.CompleteWorkOrder;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.workorders.application.usecase.FindWorkOccurrence;
import com.klaus.moply.workorders.application.usecase.FindWorkOrderById;
import com.klaus.moply.workorders.application.usecase.FindWorkOrders;

@Configuration
public class WorkOrderConfig {

	@Bean
	FindWorkOccurrence findWorkOccurrence(WorkOrderOccurrences occurrences) {
		return new FindWorkOccurrence(occurrences);
	}

	@Bean
	CreateWorkOrder createWorkOrder(WorkOrderRepository repository, CustomerRepository customers,
			CollaboratorRepository collaborators, OrganizationRepository organizations) {
		return new CreateWorkOrder(repository, customers, collaborators, organizations);
	}

	@Bean
	FindWorkOrderById findWorkOrderById(WorkOrderRepository repository, CustomerRepository customers) {
		return new FindWorkOrderById(repository, customers);
	}

	@Bean
	FindWorkOrders findWorkOrders(WorkOrderRepository repository, CustomerRepository customers) {
		return new FindWorkOrders(repository, customers);
	}

	@Bean
	CompleteWorkOrder completeWorkOrder(WorkOrderOperations operations) {
		return new CompleteWorkOrder(operations);
	}

}
