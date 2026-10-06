package com.klaus.moply.reports.infra.web.dto.response;

import java.math.BigDecimal;

public record WorkOrdersReportSummaryResponse(BigDecimal realizedAmount, BigDecimal realizedPendingAmount,
		BigDecimal workProjectionAmount) {
}
