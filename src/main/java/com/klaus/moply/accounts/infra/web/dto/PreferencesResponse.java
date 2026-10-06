package com.klaus.moply.accounts.infra.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;

public record PreferencesResponse(UUID organizationId, String name, String currencyCode, String timezone,
		DefaultWorkStatus defaultWorkStatus, java.time.LocalDate today, BigDecimal defaultHourlyRate) {
}
