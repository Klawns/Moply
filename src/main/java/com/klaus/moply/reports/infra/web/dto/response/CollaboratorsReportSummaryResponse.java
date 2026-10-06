package com.klaus.moply.reports.infra.web.dto.response;

import java.math.BigDecimal;

public record CollaboratorsReportSummaryResponse(BigDecimal allocatedTotal, BigDecimal realizedAllocatedTotal,
		BigDecimal futureAllocatedTotal, BigDecimal pendingTotal, BigDecimal realizedPendingTotal,
		BigDecimal futurePendingTotal, BigDecimal settlementsOnPeriodTotal) {
}
