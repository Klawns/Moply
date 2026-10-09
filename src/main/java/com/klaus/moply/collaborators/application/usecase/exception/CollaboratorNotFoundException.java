package com.klaus.moply.collaborators.application.usecase.exception;

import java.util.UUID;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class CollaboratorNotFoundException extends ApplicationException {

	public CollaboratorNotFoundException(UUID id) {
		super("COLLABORATOR_NOT_FOUND", "Colaborador não encontrado: " + id);
	}

}
