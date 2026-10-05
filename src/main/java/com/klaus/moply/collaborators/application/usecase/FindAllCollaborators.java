package com.klaus.moply.collaborators.application.usecase;

import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.usecase.dto.CollaboratorOutput;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllCollaborators
		implements Usecase.Contextual<FindAllCollaborators.Filter, PageResult<CollaboratorOutput>> {

	private final CollaboratorRepository repo;

	public record Filter(Boolean active, PageQuery page) {
	}

	@Override
	public PageResult<CollaboratorOutput> execute(Context context, Filter input) {
		return repo.findAll(context.organizationId(), input.active(), input.page()).map(CollaboratorOutput::fromDomain);
	}

}
