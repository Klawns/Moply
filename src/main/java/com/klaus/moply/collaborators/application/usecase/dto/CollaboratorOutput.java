package com.klaus.moply.collaborators.application.usecase.dto;

import java.util.UUID;
import java.math.BigDecimal;

import com.klaus.moply.collaborators.domain.entities.Collaborator;

public record CollaboratorOutput(UUID id, String name, String phone, boolean active, BigDecimal hourlyRate) {
	public CollaboratorOutput(UUID id, String name, String phone, boolean active) {
		this(id, name, phone, active, null);
	}

	public static CollaboratorOutput fromDomain(Collaborator collaborator) {
		return new CollaboratorOutput(collaborator.getId(), collaborator.getName().value(),
				collaborator.getPhone() == null ? null : collaborator.getPhone().value(), collaborator.isActive(),
				collaborator.getHourlyRate() == null ? null : collaborator.getHourlyRate().value());
	}
}
