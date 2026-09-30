package com.klaus.moply.collaborators.domain.exception;

public class InactiveCollaboratorException extends RuntimeException {

	public InactiveCollaboratorException() {
		super("Colaborador desativado não pode ser editado.");
	}

}
