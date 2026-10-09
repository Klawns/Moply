package com.klaus.moply.reports.application.usecase.dto;

import java.time.LocalDate;

public record ReportContext(String timezone, String currencyCode, LocalDate referenceDate) {
}
