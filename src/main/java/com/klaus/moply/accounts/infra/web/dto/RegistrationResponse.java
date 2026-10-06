package com.klaus.moply.accounts.infra.web.dto;

import java.util.UUID;

public record RegistrationResponse(UUID organizationId, UUID userId) {
}
