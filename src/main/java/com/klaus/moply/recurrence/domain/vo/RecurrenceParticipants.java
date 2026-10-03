package com.klaus.moply.recurrence.domain.vo;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;

public record RecurrenceParticipants(List<UUID> ids) {
	public RecurrenceParticipants {
		if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)
				|| new HashSet<>(ids).size() != ids.size()) {
			throw new DomainException("Condições da série inválidas.");
		}
		ids = List.copyOf(ids);
	}
}
