package com.klaus.moply.workorders.infra.web.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import com.klaus.moply.recurrence.domain.ChangeScope;

public record RescheduleRequest(LocalDate serviceDate, LocalTime startTime, ChangeScope scope, String idempotencyKey) {
}
