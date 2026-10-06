package com.klaus.moply.auth.infra.web.dto;

import java.util.UUID;

public record UserResponse(UUID userId, UUID organizationId, String email) {
}
