package com.klaus.moply.collaborators.application.usecase;

import java.util.UUID;

import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.usecase.dto.CreateCollaboratorInput;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateCollaborator implements Usecase.Contextual<CreateCollaboratorInput, UUID> {

	private final CollaboratorRepository repo;

	@Override
	public UUID execute(Context context, CreateCollaboratorInput input) {
		return repo
			.save(context.organizationId(),
					Collaborator.create(context.organizationId(), input.name(), input.phone(), input.hourlyRate()))
			.getId();
	}

}
