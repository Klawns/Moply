package com.klaus.moply.collaborators.infra.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CollaboratorRequest(@NotBlank(message = "O nome do colaborador é obrigatório.") String name,
		String phone) {
}
