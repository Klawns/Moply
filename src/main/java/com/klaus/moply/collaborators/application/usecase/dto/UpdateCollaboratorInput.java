package com.klaus.moply.collaborators.application.usecase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCollaboratorInput(UUID id, String name, String phone, BigDecimal hourlyRate,
		boolean hourlyRateProvided) {
	public UpdateCollaboratorInput(UUID id, String name, String phone) {
		this(id, name, phone, null, false);
	}
}
