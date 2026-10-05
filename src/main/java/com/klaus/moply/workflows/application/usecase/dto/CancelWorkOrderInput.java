package com.klaus.moply.workflows.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;

public record CancelWorkOrderInput(UUID id, UUID actorId, boolean confirmNoMoneyReceived, String reason) {
	public CancelWorkOrderInput {
		if (id == null) {
			throw new DomainException("Trabalho obrigatório.");
		}
	}
}
