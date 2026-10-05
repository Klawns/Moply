package com.klaus.moply.workflows.application.usecase.dto;

import java.util.UUID;

public record RecordRecurrenceCommandInput(UUID familyId, String idempotencyKey, String content, UUID actorId,
		UUID successorId) {
}
