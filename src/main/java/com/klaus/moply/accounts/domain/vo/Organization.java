package com.klaus.moply.accounts.domain.vo;

import java.time.ZoneId;
import java.util.UUID;

import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.shared.domain.exception.DomainException;

public record Organization(UUID id, String name, String timezone, DefaultWorkStatus defaultWorkStatus) {
	public Organization {
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
		return new Organization(id, name, timezone, status);
	}
}
