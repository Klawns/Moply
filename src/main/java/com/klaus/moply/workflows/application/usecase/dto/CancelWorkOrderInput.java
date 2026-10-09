package com.klaus.moply.workflows.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public record CancelWorkOrderInput(UUID id, UUID actorId, boolean confirmNoMoneyReceived, String reason) {
	public CancelWorkOrderInput {
		if (id == null) {
			throw new ApplicationException("Trabalho obrigatório.");
		}
	}
}
