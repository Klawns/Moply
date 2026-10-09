package com.klaus.moply.accounts.application.usecase.dto;

import java.math.BigDecimal;

import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;

public record UpdateAccountPreferencesInput(String timezone, DefaultWorkStatus defaultWorkStatus,
		BigDecimal defaultHourlyRate, boolean defaultHourlyRateProvided) {
	public UpdateAccountPreferencesInput(String timezone, DefaultWorkStatus defaultWorkStatus) {
		this(timezone, defaultWorkStatus, null, false);
	}
}
