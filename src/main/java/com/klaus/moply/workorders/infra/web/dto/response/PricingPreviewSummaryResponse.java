package com.klaus.moply.workorders.infra.web.dto.response;

import java.math.BigDecimal;

public record PricingPreviewSummaryResponse(BigDecimal baseTotal, BigDecimal surplusAmount, BigDecimal excessAmount) {
}
