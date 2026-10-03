package com.klaus.moply.recurrence.infra;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;

import com.klaus.moply.recurrence.application.usecase.dto.CreateSeriesInput;

import com.klaus.moply.recurrence.application.usecase.GenerateSeries;

import com.klaus.moply.recurrence.application.usecase.CreateSeries;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.shared.application.usecase.Usecase;

@Service
public class TransactionalCreateSeries extends CreateSeries {

	public TransactionalCreateSeries(RecurrenceRepository repo, CreateWorkOrder create, GenerateSeries generate) {
		super(repo, create, generate);
	}

	@Override
	@Transactional
	public RecurrenceSeries execute(Usecase.Context context, CreateSeriesInput input) {
		return super.execute(context, input);
	}

}
