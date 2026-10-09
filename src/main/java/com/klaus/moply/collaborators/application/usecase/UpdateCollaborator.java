package com.klaus.moply.collaborators.application.usecase;

import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.usecase.dto.CollaboratorOutput;
import com.klaus.moply.collaborators.application.usecase.dto.UpdateCollaboratorInput;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateCollaborator implements Usecase.Contextual<UpdateCollaboratorInput, CollaboratorOutput> {

	private final CollaboratorRepository repo;

	@Override
	public CollaboratorOutput execute(Context context, UpdateCollaboratorInput input) {
		var collaborator = repo.findById(context.organizationId(), input.id())
			.orElseThrow(() -> new CollaboratorNotFoundException(input.id()));
		return CollaboratorOutput.fromDomain(repo.save(context.organizationId(),
				collaborator.update(input.name(), input.phone(), input.hourlyRateProvided() ? input.hourlyRate()
						: collaborator.getHourlyRate() == null ? null : collaborator.getHourlyRate().value())));
	}

}
