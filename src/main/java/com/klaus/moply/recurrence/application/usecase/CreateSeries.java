package com.klaus.moply.recurrence.application.usecase;

import com.klaus.moply.recurrence.domain.FrozenWorkPricing;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.application.usecase.dto.CreateSeriesInput;
import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import com.klaus.moply.recurrence.domain.WorkTemplate;
import com.klaus.moply.recurrence.domain.vo.RecurrenceParticipants;
import com.klaus.moply.recurrence.domain.vo.RecurrencePeriod;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.service.WorkOrderPreparation;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.domain.entity.WorkAssignment;
import com.klaus.moply.workorders.domain.entity.WorkOrder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateSeries implements Usecase.Contextual<CreateSeriesInput, RecurrenceSeries> {

	private final RecurrenceRepository repository;

	private final WorkOrderPreparation preparation;

	private final GenerateSeries generate;

	public RecurrenceSeries execute(Usecase.Context context, CreateSeriesInput input) {
		validateInput(input);

		var work = prepareWork(context, input);

		var template = toTemplate(work);

		var series = RecurrenceSeries.create(context.organizationId(), input.frequency(),
				new RecurrencePeriod(input.startsOn(), input.endsOn()), template);

		series = repository.save(series);

		generate.execute(context, series.getId());
		return series;
	}

	private static void validateInput(CreateSeriesInput input) {
		if (input == null || input.work() == null) {
			throw new DomainException("Condições da série obrigatórias.");
		}
	}

	private WorkOrder prepareWork(Usecase.Context context, CreateSeriesInput input) {
		var work = input.work();
		return preparation.prepare(context,
				new CreateWorkOrderInput(work.customerId(), work.customerLocationId(), input.startsOn(),
						work.startTime(), work.description(), work.contractedHours(), work.hourlyRate(),
						work.participantIds(), work.initialStatus(), work.acceptedPricingFingerprint()));
	}

	private static WorkTemplate toTemplate(WorkOrder work) {
		return new WorkTemplate(work.customerId(), work.customerLocationId(), work.startTime(), work.description(),
				work.contractedHours(), work.hourlyRate(), work.currencyCode(),
				new RecurrenceParticipants(work.assignments().stream().map(WorkAssignment::collaboratorId).toList()),
				work.status(), new FrozenWorkPricing(work.pricing(), work.workAssignments()));
	}

}
