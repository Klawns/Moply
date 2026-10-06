package com.klaus.moply.reports.infra.web.dto.response;

import java.time.LocalDate;

public record ReportContextResponse(String timezone, String currencyCode, LocalDate referenceDate) {
}
