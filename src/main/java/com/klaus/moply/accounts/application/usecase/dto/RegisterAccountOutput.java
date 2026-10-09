package com.klaus.moply.accounts.application.usecase.dto;

import java.util.UUID;

public record RegisterAccountOutput(UUID organizationId, UUID userId) {
}
