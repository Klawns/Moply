package com.klaus.moply.recurrence.application.usecase;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;

import com.klaus.moply.recurrence.application.usecase.exception.SeriesNotFoundException;

import java.util.UUID;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindSeries implements Usecase.Contextual<UUID, RecurrenceSeries> {

	private final RecurrenceRepository repository;

	public RecurrenceSeries execute(Usecase.Context context, UUID id) {
		return repository.find(context.organizationId(), id).orElseThrow(SeriesNotFoundException::new);
	}

}
