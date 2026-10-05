package com.klaus.moply.recurrence.domain.vo;

import java.util.UUID;
import com.klaus.moply.shared.domain.exception.DomainException;

/** A half-open interval of stable positions in a recurrence family. */
public record SeriesVersion(UUID familyId, UUID previousSeriesId, long firstPosition, Long untilPosition) {
	public SeriesVersion {
		if (familyId == null || firstPosition < 0 || (untilPosition != null && untilPosition < firstPosition))
			throw new DomainException("Versão de recorrência inválida.");
	}

	public boolean contains(long position) {
		return position >= firstPosition && (untilPosition == null || position < untilPosition);
	}

	public SeriesVersion closeAt(long position) {
		long limit = Math.max(firstPosition, position);
		return new SeriesVersion(familyId, previousSeriesId, firstPosition,
				untilPosition == null ? limit : Math.min(untilPosition, limit));
	}
}
