package com.klaus.moply.recurrence.infra.web.dto.response;

import java.math.BigDecimal;

public record WorkConditionsPricingResponse(BigDecimal contractedHours, BigDecimal hourlyRate, String currencyCode) {
}
