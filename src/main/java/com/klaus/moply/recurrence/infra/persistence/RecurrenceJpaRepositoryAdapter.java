package com.klaus.moply.recurrence.infra.persistence;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;

import com.klaus.moply.recurrence.application.usecase.exception.SeriesNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.data.domain.PageRequest;

import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurrenceJpaRepositoryAdapter implements RecurrenceRepository {

	private final RecurrenceSeriesJpaRepository repository;

	@Transactional
	public RecurrenceSeries save(RecurrenceSeries series) {
		return repository.saveAndFlush(RecurrenceSeriesEntity.from(series)).toDomain();
	}

	public Optional<RecurrenceSeries> find(UUID account, UUID id) {
		return repository.findByOrganizationIdAndId(account, id).map(RecurrenceSeriesEntity::toDomain);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public RecurrenceSeries lock(UUID account, UUID id) {
		return repository.lock(account, id).orElseThrow(SeriesNotFoundException::new).toDomain();
	}

	public List<Reference> nextBatch(UUID after, int size) {
		return repository.nextBatch(after, PageRequest.of(0, size))
			.stream()
			.map(r -> new Reference(r.getOrganizationId(), r.getId()))
			.toList();
	}

}
