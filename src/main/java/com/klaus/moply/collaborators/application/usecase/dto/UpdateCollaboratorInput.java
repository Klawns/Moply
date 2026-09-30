package com.klaus.moply.collaborators.application.usecase.dto;

import java.util.UUID;

public record UpdateCollaboratorInput(UUID id, String name, String phone) {
}
