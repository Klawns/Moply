package com.klaus.moply.workorders.infra.persistence;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderOccurrencesJpaAdapter implements WorkOrderOccurrences {

	private final WorkOrderJpaRepository repository;

	@Override
	public Reference reference(UUID organizationId, UUID workId) {
		var reference = repository.occurrenceReference(organizationId, workId)
			.orElseThrow(() -> new WorkOrderNotFoundException(workId));
		return toReference(reference);
	}

	@Override
	public List<Reference> inSeries(UUID organizationId, UUID seriesId) {
		return repository.seriesOccurrences(organizationId, seriesId)
			.stream()
			.map(WorkOrderOccurrencesJpaAdapter::toReference)
			.toList();
	}

	@Override
	public Set<LocalDate> findDates(UUID organizationId, UUID seriesId, LocalDate from, LocalDate until) {
		return new HashSet<>(repository.findOccurrenceDates(organizationId, seriesId, from, until));
	}

	private static Reference toReference(WorkOrderJpaRepository.OccurrenceReference reference) {
		return new Reference(reference.getId(), reference.getSeriesId(), reference.getOriginalDate());
	}

	@Transactional(propagation = Propagation.MANDATORY)
	@Override
	public void link(UUID organizationId, UUID workId, UUID seriesId, LocalDate originalDate) {
		var work = repository.findForUpdate(organizationId, workId)
			.orElseThrow(() -> new WorkOrderNotFoundException(workId));
		if (work.getRecurrenceSeriesId() != null || seriesId == null || originalDate == null
				|| !originalDate.equals(work.getServiceDate()))
			throw new IllegalArgumentException("Vínculo de ocorrência inválido.");
		work.setRecurrenceSeriesId(seriesId);
		work.setOccurrenceDate(originalDate);
		repository.saveAndFlush(work);
	}

}
