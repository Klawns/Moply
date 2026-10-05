package com.klaus.moply.collaborators.domain.entities;

import java.util.Objects;
import java.math.BigDecimal;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import java.util.UUID;

import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.collaborators.domain.vo.CollaboratorName;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Phone;

import lombok.Getter;

@Getter
public final class Collaborator {

	private final UUID id;

	private final UUID organizationId;

	private final CollaboratorName name;

	private final Phone phone;

	private final boolean active;

	private final long version;

	private final HourlyRate hourlyRate;

	private Collaborator(UUID id, UUID organizationId, CollaboratorName name, Phone phone, boolean active, long version,
			BigDecimal hourlyRate) {
		this.hourlyRate = hourlyRate == null ? null : new HourlyRate(hourlyRate);
		this.id = id;
		this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
		this.name = name;
		this.phone = phone;
		this.active = active;
		this.version = version;
	}

	public static Collaborator create(UUID organizationId, String name, String phone) {
		return create(organizationId, name, phone, null);
	}

	public static Collaborator create(UUID organizationId, String name, String phone, BigDecimal hourlyRate) {
		return new Collaborator(null, organizationId, new CollaboratorName(name), Phone.ofNullable(phone), true, 0,
				hourlyRate);
	}

	public static Collaborator restore(UUID id, UUID organizationId, String name, String phone, boolean active,
			long version) {
		return restore(id, organizationId, name, phone, active, version, null);
	}

	public static Collaborator restore(UUID id, UUID organizationId, String name, String phone, boolean active,
			long version, BigDecimal hourlyRate) {
		if (id == null) {
			throw new DomainException("O ID do colaborador é obrigatório para reconstituição.");
		}
		return new Collaborator(id, organizationId, new CollaboratorName(name), Phone.ofNullable(phone), active,
				version, hourlyRate);
	}

	public Collaborator update(String name, String phone) {
		return update(name, phone, hourlyRate == null ? null : hourlyRate.value());
	}

	public Collaborator update(String name, String phone, BigDecimal hourlyRate) {
		if (!active) {
			throw new InactiveCollaboratorException();
		}
		return new Collaborator(id, organizationId, new CollaboratorName(name), Phone.ofNullable(phone), active,
				version, hourlyRate);
	}

	public Collaborator deactivate() {
		if (!active) {
			return this;
		}
		return new Collaborator(id, organizationId, name, phone, false, version,
				hourlyRate == null ? null : hourlyRate.value());
	}

}
