package com.klaus.moply.recurrence.infra.transaction;

import org.springframework.transaction.annotation.Isolation;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;

import com.klaus.moply.recurrence.application.usecase.dto.CreateSeriesInput;

import com.klaus.moply.recurrence.application.usecase.GenerateSeries;

import com.klaus.moply.recurrence.application.usecase.CreateSeries;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import com.klaus.moply.workorders.application.service.WorkOrderPreparation;
import com.klaus.moply.shared.application.usecase.Usecase;

@Service
public class TransactionalCreateSeries extends CreateSeries {

	public TransactionalCreateSeries(RecurrenceRepository repo, WorkOrderPreparation preparation,
			GenerateSeries generate) {
		super(repo, preparation, generate);
	}

	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public RecurrenceSeries execute(Usecase.Context context, CreateSeriesInput input) {
		return super.execute(context, input);
	}

}
