package com.klaus.moply.payments.infra.web.dto.response;

import java.math.BigDecimal;

public record PaymentBalanceResponse(BigDecimal allocatedAmount, BigDecimal recordedAmount,
		BigDecimal remainingAmount, boolean requiresAttention) {
}
