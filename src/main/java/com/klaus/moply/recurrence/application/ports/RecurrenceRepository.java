package com.klaus.moply.recurrence.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.klaus.moply.recurrence.domain.RecurrenceSeries;

public interface RecurrenceRepository {

	RecurrenceSeries save(RecurrenceSeries series);

	Optional<RecurrenceSeries> find(UUID account, UUID id);

	RecurrenceSeries lock(UUID account, UUID id);

	List<Reference> nextBatch(UUID after, int size);

	record Reference(UUID organizationId, UUID id) {
	}

}
