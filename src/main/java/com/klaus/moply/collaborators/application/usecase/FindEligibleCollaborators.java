package com.klaus.moply.collaborators.application.usecase;

import java.util.List;

import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.usecase.dto.CollaboratorOutput;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindEligibleCollaborators implements Usecase.Contextual<Void, List<CollaboratorOutput>> {

	private final CollaboratorRepository repo;

	@Override
	public List<CollaboratorOutput> execute(Context context, Void input) {
		return repo.findAll(context.organizationId(), true).stream().map(CollaboratorOutput::fromDomain).toList();
	}

}
