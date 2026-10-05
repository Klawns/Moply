package com.klaus.moply.collaborators.application.usecase.dto;

import java.math.BigDecimal;

public record CreateCollaboratorInput(String name, String phone, BigDecimal hourlyRate) {
	public CreateCollaboratorInput(String name, String phone) {
		this(name, phone, null);
	}
}
