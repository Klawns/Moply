package com.klaus.moply.recurrence.infra.web.dto.response;

import java.util.UUID;

public record SeriesLineageResponse(UUID familyId, UUID previousSeriesId, long firstPosition, Long untilPosition) {
}
