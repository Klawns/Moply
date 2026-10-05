package com.klaus.moply.recurrence.infra.config;

import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;

import com.klaus.moply.recurrence.application.usecase.FindOccurrenceHistory;

import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;

import com.klaus.moply.recurrence.application.usecase.FindSeries;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecurrenceConfig {

	@Bean
	FindOccurrenceHistory findOccurrenceHistory(RecurrenceChanges changes, RecurrenceRepository series,
			WorkOrderOccurrences occurrences) {
		return new FindOccurrenceHistory(changes, series, occurrences);
	}

	@Bean
	FindSeries findSeries(RecurrenceRepository repository) {
		return new FindSeries(repository);
	}

}
