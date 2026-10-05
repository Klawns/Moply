package com.klaus.moply.reports.application.usecase;

import java.time.LocalDate;

final class ReportRealization {

	private ReportRealization() {
	}

	static boolean isRealized(String status, LocalDate serviceDate, LocalDate referenceDate) {
		return "COMPLETED".equals(status) && !serviceDate.isAfter(referenceDate);
	}

}
