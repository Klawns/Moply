package com.klaus.moply.workflows.application.usecase.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.klaus.moply.recurrence.domain.ChangeScope;

public record RescheduleSelectedWorkOrderInput(UUID workId, UUID actorId, LocalDate serviceDate, LocalTime startTime,
		ChangeScope scope, String idempotencyKey) {
}