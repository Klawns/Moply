package com.klaus.moply.accounts.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;

public record GetAccountPreferencesOutput(UUID organizationId, String name, String currencyCode, String timezone,
		DefaultWorkStatus defaultWorkStatus, LocalDate today, BigDecimal defaultHourlyRate) {
}
