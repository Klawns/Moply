package com.klaus.moply.payments.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.payments.application.usecase.ListWorkOrderPayments;
import com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment;
import com.klaus.moply.payments.application.usecase.RecordCollaboratorPayment;
import com.klaus.moply.payments.application.usecase.ListCollaboratorPayments;
import com.klaus.moply.payments.application.usecase.ReverseCollaboratorPayment;
import com.klaus.moply.payments.application.usecase.GetCollaboratorPaymentSummary;
import com.klaus.moply.payments.application.usecase.ReverseWorkOrderPayment;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;

@Configuration
public class PaymentConfig {

	@Bean
	RecordWorkOrderPayment recordWorkOrderPayment(WorkOrderOperations works, WorkOrderPaymentRepository payments,
			OrganizationRepository accounts, Clock clock) {
		return new RecordWorkOrderPayment(works, payments, accounts, clock);
	}

	@Bean
	ReverseWorkOrderPayment reverseWorkOrderPayment(WorkOrderOperations works, WorkOrderPaymentRepository payments,
			Clock clock) {
		return new ReverseWorkOrderPayment(works, payments, clock);
	}

	@Bean
	ListWorkOrderPayments listWorkOrderPayments(WorkOrderPaymentRepository payments, WorkOrderRepository works) {
		return new ListWorkOrderPayments(payments, works);
	}

	@Bean
	RecordCollaboratorPayment recordCollaboratorPayment(WorkOrderOperations works,
			CollaboratorPaymentRepository payments, OrganizationRepository organizations, Clock clock) {
		return new RecordCollaboratorPayment(works, payments, organizations, clock);
	}

	@Bean
	ListCollaboratorPayments listCollaboratorPayments(CollaboratorPaymentRepository payments,
			WorkOrderRepository works) {
		return new ListCollaboratorPayments(payments, works);
	}

	@Bean
	ReverseCollaboratorPayment reverseCollaboratorPayment(WorkOrderOperations works,
			CollaboratorPaymentRepository payments, Clock clock) {
		return new ReverseCollaboratorPayment(works, payments, clock);
	}

	@Bean
	GetCollaboratorPaymentSummary getCollaboratorPaymentSummary(WorkOrderRepository works,
			CollaboratorPaymentRepository payments, CollaboratorRepository collaborators,
			OrganizationRepository organizations, Clock clock) {
		return new GetCollaboratorPaymentSummary(works, payments, collaborators, organizations, clock);
	}

}
