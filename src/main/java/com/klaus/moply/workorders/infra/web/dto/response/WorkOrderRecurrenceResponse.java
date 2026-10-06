package com.klaus.moply.workorders.infra.web.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record WorkOrderRecurrenceResponse(UUID seriesId, LocalDate occurrenceDate) {
}
