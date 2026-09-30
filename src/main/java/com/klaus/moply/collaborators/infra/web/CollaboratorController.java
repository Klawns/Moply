package com.klaus.moply.collaborators.infra.web;

import java.net.URI;
import java.util.List;
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
import com.klaus.moply.collaborators.infra.web.dto.CollaboratorRequest;
import com.klaus.moply.collaborators.infra.web.dto.CollaboratorResponse;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/collaborators")
@RequiredArgsConstructor
public class CollaboratorController {

	private final CreateCollaborator create;

	private final FindAllCollaborators findAll;

	private final FindCollaboratorById findById;

	private final UpdateCollaborator update;

	private final DeactivateCollaborator deactivate;

	@PostMapping
	public ResponseEntity<UUID> create(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody CollaboratorRequest request) {
		var id = create.execute(new Context(principal.getOrganizationId()),
				new CreateCollaboratorInput(request.name(), request.phone()));
		return ResponseEntity.created(URI.create("/api/v1/collaborators/" + id)).body(id);
	}

	@GetMapping
	public List<CollaboratorResponse> list(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestParam(required = false) Boolean active) {
		return findAll.execute(new Context(principal.getOrganizationId()), active)
			.stream()
			.map(CollaboratorResponse::from)
			.toList();
	}

	@GetMapping("/{id}")
	public CollaboratorResponse find(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		return CollaboratorResponse.from(findById.execute(new Context(principal.getOrganizationId()), id));
	}

	@PutMapping("/{id}")
	public CollaboratorResponse update(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id,
			@Valid @RequestBody CollaboratorRequest request) {
		return CollaboratorResponse.from(update.execute(new Context(principal.getOrganizationId()),
				new UpdateCollaboratorInput(id, request.name(), request.phone())));
	}

	@PostMapping("/{id}/deactivate")
	public ResponseEntity<Void> deactivate(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		deactivate.execute(new Context(principal.getOrganizationId()), id);
		return ResponseEntity.noContent().build();
	}

}
