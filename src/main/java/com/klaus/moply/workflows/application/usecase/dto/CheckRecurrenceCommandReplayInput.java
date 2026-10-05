package com.klaus.moply.workflows.application.usecase.dto;

import java.util.UUID;

public record CheckRecurrenceCommandReplayInput(UUID familyId, String idempotencyKey, String content) {
}
