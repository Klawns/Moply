package com.klaus.moply.recurrence.infra;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;

import com.klaus.moply.recurrence.application.usecase.GenerateSeries;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.klaus.moply.shared.application.usecase.Usecase.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "moply.recurrence.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RecurrenceScheduler {

	private final RecurrenceRepository repository;

	private final GenerateSeries generate;

	/**
	 * Deliberately no encompassing transaction: a failed series cannot poison the batch.
	 */
	@Scheduled(fixedDelayString = "${moply.recurrence.scheduler.delay:PT15M}",
			initialDelayString = "${moply.recurrence.scheduler.initial-delay:PT0S}")
	public void run() {
		UUID after = new UUID(0, 0);
		int succeeded = 0, failed = 0;
		long started = System.nanoTime();
		while (true) {
			var batch = repository.nextBatch(after, 100);
			if (batch.isEmpty())
				break;
			for (var ref : batch) {
				long seriesStarted = System.nanoTime();
				try {
					var result = generate.execute(new Context(ref.organizationId()), ref.id());
					succeeded++;
					log.info("recurrence account={} series={} from={} until={} created={} existing={} durationMs={}",
							ref.organizationId(), ref.id(), result.from(), result.until(), result.created(),
							result.existing(), (System.nanoTime() - seriesStarted) / 1_000_000);
				}
				catch (RuntimeException error) {
					failed++;
					log.error("recurrence failed account={} series={} durationMs={}", ref.organizationId(), ref.id(),
							(System.nanoTime() - seriesStarted) / 1_000_000, error);
				}
			}
			after = batch.getLast().id();
		}
		log.info("recurrence batch succeeded={} failed={} durationMs={}", succeeded, failed,
				(System.nanoTime() - started) / 1_000_000);
	}

}
