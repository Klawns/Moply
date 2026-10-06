package com.klaus.moply.payments.infra.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReversePaymentRequest(boolean confirmNoMoneyReceived, @NotBlank String reason) {
}
