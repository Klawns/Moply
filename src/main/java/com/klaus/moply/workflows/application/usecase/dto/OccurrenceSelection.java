package com.klaus.moply.workflows.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.shared.domain.exception.DomainException;

/**
 * Validates the key used by the PostgreSQL recurrence command history to detect replays
 * and content conflicts.
 */
public record OccurrenceSelection(UUID workId, UUID actorId, ChangeScope scope, String idempotencyKey) {
	public OccurrenceSelection {
		if (workId == null || actorId == null || scope == null || idempotencyKey == null || idempotencyKey.isBlank()
				|| idempotencyKey.length() > 255) {
			throw new DomainException("Ocorrência, responsável, alcance e chave de idempotência são obrigatórios.");
		}
		idempotencyKey = idempotencyKey.strip();
	}
}
