package com.klaus.moply.collaborators.application.usecase;

import com.klaus.moply.collaborators.application.usecase.dto.FindAllCollaboratorsFilter;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.usecase.dto.CollaboratorOutput;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllCollaborators
		implements Usecase.Contextual<FindAllCollaboratorsFilter, PageResult<CollaboratorOutput>> {

	private final CollaboratorRepository repo;

	@Override
	public PageResult<CollaboratorOutput> execute(Context context, FindAllCollaboratorsFilter input) {
		return repo.findAll(context.organizationId(), input.active(), input.page()).map(CollaboratorOutput::fromDomain);
	}

}
