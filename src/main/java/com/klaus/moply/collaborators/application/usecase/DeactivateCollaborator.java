package com.klaus.moply.collaborators.application.usecase;

import java.util.UUID;

import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeactivateCollaborator implements Usecase.Contextual<UUID, Void> {

	private final CollaboratorRepository repo;

	@Override
	public Void execute(Context context, UUID input) {
		var collaborator = repo.findById(context.organizationId(), input)
			.orElseThrow(() -> new CollaboratorNotFoundException(input));
		if (collaborator.isActive())
			repo.save(context.organizationId(), collaborator.deactivate());
		return null;
	}

}
