package com.klaus.moply.workorders.infra.web.dto.response;

import java.math.BigDecimal;

public record WorkPricingResponse(BigDecimal contractedHours, BigDecimal hourlyRate, String currencyCode,
		BigDecimal totalAmount, int allocationPolicyVersion) {
}
