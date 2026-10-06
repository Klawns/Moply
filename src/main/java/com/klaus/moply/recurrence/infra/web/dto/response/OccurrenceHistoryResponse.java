package com.klaus.moply.recurrence.infra.web.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.klaus.moply.recurrence.application.usecase.dto.OccurrenceHistoryOutput;

public record OccurrenceHistoryResponse(UUID operationId, UUID actorId, Instant at, String reason,
		UUID replacementWorkId, OccurrenceHistoryTargetResponse target, ServiceDateChangeResponse serviceDateChange) {

	public static OccurrenceHistoryResponse from(OccurrenceHistoryOutput h) {
		return new OccurrenceHistoryResponse(h.operationId(), h.actorId(), h.at(), h.reason(), h.replacementWorkId(),
				new OccurrenceHistoryTargetResponse(h.position(), h.targetSeriesId(), h.targetOccurrenceDate()),
				new ServiceDateChangeResponse(h.serviceDateBefore(), h.serviceDateAfter()));
	}

}
