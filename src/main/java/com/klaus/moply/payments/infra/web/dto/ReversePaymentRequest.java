package com.klaus.moply.payments.infra.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ReversePaymentRequest(boolean confirmNoMoneyReceived, @NotBlank String reason) {
}
