package com.klaus.moply.collaborators.infra.web.dto;

import java.util.UUID;

import com.klaus.moply.collaborators.application.usecase.dto.CollaboratorOutput;

public record CollaboratorResponse(UUID id, String name, String phone, boolean active) {
	public static CollaboratorResponse from(CollaboratorOutput output) {
		return new CollaboratorResponse(output.id(), output.name(), output.phone(), output.active());
	}
}
