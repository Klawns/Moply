package com.klaus.moply.workorders.infra.persistence;

import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderOccurrencesJpaAdapter implements WorkOrderOccurrences {

	private final WorkOrderJpaRepository repository;

	public Set<LocalDate> findDates(UUID account, UUID series, LocalDate from, LocalDate until) {
		return new HashSet<>(repository.findOccurrenceDates(account, series, from, until));
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void link(UUID account, UUID workId, UUID series, LocalDate originalDate) {
		var work = repository.findForUpdate(account, workId).orElseThrow(() -> new WorkOrderNotFoundException(workId));
		if (work.getRecurrenceSeriesId() != null || series == null || originalDate == null
				|| !originalDate.equals(work.getServiceDate()))
			throw new IllegalArgumentException("Vínculo de ocorrência inválido.");
		work.setRecurrenceSeriesId(series);
		work.setOccurrenceDate(originalDate);
		repository.saveAndFlush(work);
	}

}
