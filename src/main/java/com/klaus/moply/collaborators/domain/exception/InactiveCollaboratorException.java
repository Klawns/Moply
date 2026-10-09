package com.klaus.moply.collaborators.domain.exception;

import com.klaus.moply.shared.domain.exception.DomainException;

public class InactiveCollaboratorException extends DomainException {

	public InactiveCollaboratorException() {
		super("INACTIVE_COLLABORATOR", "Colaborador desativado não pode ser editado.");
	}

}
