package com.klaus.moply.collaborators.application.exception;

import java.util.UUID;

public class CollaboratorNotFoundException extends RuntimeException {

	public CollaboratorNotFoundException(UUID id) {
		super("Colaborador não encontrado: " + id);
	}

}
