package com.klaus.moply.recurrence.infra;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;

import com.klaus.moply.recurrence.application.usecase.GenerateSeries;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.shared.application.usecase.Usecase;

@Service
@lombok.extern.slf4j.Slf4j
public class TransactionalGenerateSeries extends GenerateSeries {

	public TransactionalGenerateSeries(RecurrenceRepository repo, OrganizationRepository accounts,
			WorkOrderOccurrences occurrences, CreateWorkOrder create, Clock clock) {
		super(repo, accounts, occurrences, create, clock);
	}

	/**
	 * Joins initial creation, otherwise starts one independent transaction per scheduler
	 * call.
	 */
	@Override
	@Transactional
	public Result execute(Usecase.Context context, UUID id) {
		var result = super.execute(context, id);
		org.springframework.transaction.support.TransactionSynchronizationManager
			.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
				@Override
				public void afterCommit() {
					log.info("recurrence committed account={} series={} from={} until={} created={} existing={}",
							result.organizationId(), result.seriesId(), result.from(), result.until(), result.created(),
							result.existing());
				}
			});
		return result;
	}

}
