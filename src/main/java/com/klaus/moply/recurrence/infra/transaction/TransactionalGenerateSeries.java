package com.klaus.moply.recurrence.infra.transaction;

import com.klaus.moply.recurrence.application.usecase.dto.GenerateSeriesResult;
import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;

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
			WorkOrderOccurrences occurrences, CreateWorkOrder create, Clock clock, RecurrenceChanges changes) {
		super(repo, accounts, occurrences, create, clock, changes);
	}

	/**
	 * Joins initial creation, otherwise starts one independent transaction per scheduler
	 * call.
	 */
	@Override
	@Transactional
	public GenerateSeriesResult execute(Usecase.Context context, UUID id) {
		var result = super.execute(context, id);
		org.springframework.transaction.support.TransactionSynchronizationManager
			.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
				@Override
				public void afterCommit() {
					log.atInfo()
						.addKeyValue("event", "recurrence.committed")
						.addKeyValue("accountId", result.organizationId())
						.addKeyValue("seriesId", result.seriesId())
						.addKeyValue("from", result.from())
						.addKeyValue("until", result.until())
						.addKeyValue("created", result.created())
						.addKeyValue("existing", result.existing())
						.log("Recorrência confirmada");
				}
			});
		return result;
	}

}
