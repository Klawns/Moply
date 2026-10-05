package com.klaus.moply.accounts.domain.vo;

import java.time.ZoneId;
import java.math.BigDecimal;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import java.util.UUID;

import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.shared.domain.exception.DomainException;

public record Organization(UUID id, String name, String timezone, DefaultWorkStatus defaultWorkStatus,
		BigDecimal defaultHourlyRate) {
	public Organization(UUID id, String name, String timezone, DefaultWorkStatus status) {
		this(id, name, timezone, status, null);
	}

	public Organization {
		defaultHourlyRate = defaultHourlyRate == null ? null : new HourlyRate(defaultHourlyRate).value();
		if (id == null || name == null || name.isBlank() || defaultWorkStatus == null) {
			throw new DomainException("Conta inválida.");
		}
		name = name.strip();
		if (timezone == null || !ZoneId.getAvailableZoneIds().contains(timezone)) {
			throw new DomainException("Informe um fuso IANA válido.");
		}
	}

	public static Organization create(String name, String timezone) {
		return new Organization(UUID.randomUUID(), name, timezone, DefaultWorkStatus.SCHEDULED);
	}

	public String currencyCode() {
		return "GBP";
	}

	public Organization withPreferences(String timezone, DefaultWorkStatus status) {
		return withPreferences(timezone, status, defaultHourlyRate);
	}

	public Organization withPreferences(String timezone, DefaultWorkStatus status, BigDecimal hourlyRate) {
		return new Organization(id, name, timezone, status, hourlyRate);
	}
}
