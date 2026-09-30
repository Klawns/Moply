package com.klaus.moply.collaborators.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.collaborators.domain.entities.Collaborator;

public record CollaboratorOutput(UUID id, String name, String phone, boolean active) {
	public static CollaboratorOutput fromDomain(Collaborator collaborator) {
		return new CollaboratorOutput(collaborator.getId(), collaborator.getName().value(),
				collaborator.getPhone() == null ? null : collaborator.getPhone().value(), collaborator.isActive());
	}
}
