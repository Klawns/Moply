package com.klaus.moply.recurrence.infra;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;

import com.klaus.moply.recurrence.application.usecase.FindSeries;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecurrenceConfig {

	@Bean
	FindSeries findSeries(RecurrenceRepository repository) {
		return new FindSeries(repository);
	}

}
