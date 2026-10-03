package com.klaus.moply.payments.infra.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ReverseCollaboratorPaymentRequest(boolean confirmNotActuallyPaid, @NotBlank String reason) {
}
