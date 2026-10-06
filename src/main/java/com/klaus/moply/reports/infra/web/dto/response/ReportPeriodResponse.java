package com.klaus.moply.reports.infra.web.dto.response;

import java.time.LocalDate;

public record ReportPeriodResponse(LocalDate from, LocalDate to) {
}
