package com.klaus.moply.collaborators.application.usecase;

import java.util.UUID;

import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.usecase.dto.CollaboratorOutput;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindCollaboratorById implements Usecase.Contextual<UUID, CollaboratorOutput> {

	private final CollaboratorRepository repo;

	@Override
	public CollaboratorOutput execute(Context context, UUID input) {
		return CollaboratorOutput.fromDomain(repo.findById(context.organizationId(), input)
			.orElseThrow(() -> new CollaboratorNotFoundException(input)));
	}

}
