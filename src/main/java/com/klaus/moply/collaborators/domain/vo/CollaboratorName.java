package com.klaus.moply.collaborators.domain.vo;

import com.klaus.moply.shared.domain.exception.DomainException;

public record CollaboratorName(String value) {
	public CollaboratorName {
		if (value == null || value.isBlank()) {
			throw new DomainException("O nome do colaborador é obrigatório.");
		}
		value = value.strip();
	}
}
