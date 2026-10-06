package com.klaus.moply.collaborators.infra.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.collaborators.application.usecase.CreateCollaborator;
import com.klaus.moply.collaborators.application.usecase.DeactivateCollaborator;
import com.klaus.moply.collaborators.application.usecase.FindAllCollaborators;
import com.klaus.moply.collaborators.application.usecase.FindCollaboratorById;
import com.klaus.moply.collaborators.application.usecase.UpdateCollaborator;
import com.klaus.moply.collaborators.application.usecase.dto.CreateCollaboratorInput;
import com.klaus.moply.collaborators.application.usecase.dto.UpdateCollaboratorInput;
import com.klaus.moply.collaborators.infra.web.api.CollaboratorApi;
import com.klaus.moply.collaborators.infra.web.dto.CollaboratorRequest;
import com.klaus.moply.collaborators.infra.web.dto.CollaboratorResponse;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.infra.web.PageQueryRequest;
import com.klaus.moply.shared.infra.web.dto.PageResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/collaborators")
@RequiredArgsConstructor
public class CollaboratorController implements CollaboratorApi {

	private final CreateCollaborator create;

	private final FindAllCollaborators findAll;

	private final FindCollaboratorById findById;

	private final UpdateCollaborator update;

	private final DeactivateCollaborator deactivate;

	@PostMapping
	@Override
	public ResponseEntity<UUID> create(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody CollaboratorRequest request) {
		var id = create.execute(new Context(principal.getOrganizationId()),
				new CreateCollaboratorInput(request.name(), request.phone(), request.hourlyRate()));
		return ResponseEntity.created(URI.create("/api/v1/collaborators/" + id)).body(id);
	}

	@GetMapping
	@Override
	public PageResponse<CollaboratorResponse> list(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestParam(required = false) Boolean active, @RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size, @RequestParam(required = false) String sort,
			@RequestParam(required = false) String direction) {
		return PageResponse.from(
				findAll.execute(new Context(principal.getOrganizationId()),
						new FindAllCollaborators.Filter(active, PageQueryRequest.toQuery(page, size, sort, direction))),
				CollaboratorResponse::from);
	}

	@GetMapping("/{id}")
	@Override
	public CollaboratorResponse find(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		return CollaboratorResponse.from(findById.execute(new Context(principal.getOrganizationId()), id));
	}

	@PutMapping("/{id}")
	@Override
	public CollaboratorResponse update(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id,
			@Valid @RequestBody CollaboratorRequest request) {
		return CollaboratorResponse
			.from(update.execute(new Context(principal.getOrganizationId()), new UpdateCollaboratorInput(id,
					request.name(), request.phone(), request.hourlyRate(), request.hourlyRateProvided())));
	}

	@PostMapping("/{id}/deactivate")
	@Override
	public ResponseEntity<Void> deactivate(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		deactivate.execute(new Context(principal.getOrganizationId()), id);
		return ResponseEntity.noContent().build();
	}

}
