package com.klaus.moply.workorders.infra.config;

import com.klaus.moply.workorders.infra.transaction.TransactionalCreateWorkOrder;

import com.klaus.moply.workorders.application.usecase.PreviewWorkOrderPricing;

import com.klaus.moply.workorders.application.service.WorkOrderPreparation;
import com.klaus.moply.workorders.infra.transaction.TransactionalPreviewWorkOrderPricing;

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
	WorkOrderPreparation workOrderPreparation(CustomerRepository customers, CollaboratorRepository collaborators,
			OrganizationRepository organizations) {
		return new WorkOrderPreparation(customers, collaborators, organizations);
	}

	@Bean
	CreateWorkOrder createWorkOrder(WorkOrderRepository repository, CustomerRepository customers,
			WorkOrderPreparation preparation) {
		return new TransactionalCreateWorkOrder(repository, customers, preparation);
	}

	@Bean
	PreviewWorkOrderPricing previewWorkOrderPricing(WorkOrderPreparation preparation) {
		return new TransactionalPreviewWorkOrderPricing(preparation);
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
