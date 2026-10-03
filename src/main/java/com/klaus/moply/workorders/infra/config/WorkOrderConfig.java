package com.klaus.moply.workorders.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.CompleteWorkOrder;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.workorders.application.usecase.FindWorkOrderById;
import com.klaus.moply.workorders.application.usecase.FindWorkOrders;

@Configuration
public class WorkOrderConfig {

	@Bean
	CreateWorkOrder createWorkOrder(WorkOrderRepository r, CustomerRepository c, CollaboratorRepository p,
			OrganizationRepository a) {
		return new CreateWorkOrder(r, c, p, a);
	}

	@Bean
	FindWorkOrderById findWorkOrderById(WorkOrderRepository r, CustomerRepository c) {
		return new FindWorkOrderById(r, c);
	}

	@Bean
	FindWorkOrders findWorkOrders(WorkOrderRepository r, CustomerRepository c) {
		return new FindWorkOrders(r, c);
	}

	@Bean
	CompleteWorkOrder completeWorkOrder(WorkOrderOperations operations) {
		return new CompleteWorkOrder(operations);
	}

}
