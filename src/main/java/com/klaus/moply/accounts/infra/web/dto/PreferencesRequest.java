package com.klaus.moply.accounts.infra.web.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;

public class PreferencesRequest {

	private String timezone;

	private DefaultWorkStatus defaultWorkStatus;

	private BigDecimal defaultHourlyRate;

	private boolean defaultHourlyRateProvided;

	public PreferencesRequest() {
	}

	public String timezone() {
		return timezone;
	}

	public DefaultWorkStatus defaultWorkStatus() {
		return defaultWorkStatus;
	}

	public BigDecimal defaultHourlyRate() {
		return defaultHourlyRate;
	}

	public boolean defaultHourlyRateProvided() {
		return defaultHourlyRateProvided;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public void setDefaultWorkStatus(DefaultWorkStatus status) {
		this.defaultWorkStatus = status;
	}

	@JsonSetter("defaultHourlyRate")
	public void setDefaultHourlyRate(BigDecimal value) {
		this.defaultHourlyRate = value;
		this.defaultHourlyRateProvided = true;
	}

}
