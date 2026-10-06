package com.klaus.moply.workorders.infra.web.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record WorkOrderScheduleResponse(LocalDate serviceDate, LocalTime startTime) {
}
