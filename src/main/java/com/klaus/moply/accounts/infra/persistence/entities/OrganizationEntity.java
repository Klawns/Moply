package com.klaus.moply.accounts.infra.persistence.entities;

import java.util.UUID;
import java.math.BigDecimal;

import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_organization")
@Getter
@NoArgsConstructor
public class OrganizationEntity {

	@Id
	private UUID id;

	@Column(nullable = false, columnDefinition = "text")
	private String name;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(nullable = false, columnDefinition = "text")
	private String timezone;

	@Enumerated(EnumType.STRING)
	@Column(name = "default_work_status", nullable = false, length = 20)
	private DefaultWorkStatus defaultWorkStatus;

	@Column(name = "default_hourly_rate", columnDefinition = "numeric")
	private BigDecimal defaultHourlyRate;

	public OrganizationEntity(Organization organization) {
		id = organization.id();
		name = organization.name();
		currencyCode = organization.currencyCode();
		updatePreferences(organization);
	}

	public void updatePreferences(Organization organization) {
		timezone = organization.timezone();
		defaultWorkStatus = organization.defaultWorkStatus();
		defaultHourlyRate = organization.defaultHourlyRate();
	}

	public Organization toDomain() {
		return new Organization(id, name, timezone, defaultWorkStatus, defaultHourlyRate);
	}

}
