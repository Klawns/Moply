package com.klaus.moply.reports.application.usecase.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public record ReportPeriod(LocalDate from, LocalDate to, UUID customerId, PageQuery page) {
	public ReportPeriod(LocalDate from, LocalDate to, UUID customerId) {
		this(from, to, customerId, PageQuery.defaults());
	}

	public ReportPeriod {
		if (from == null || to == null || from.isAfter(to))
			throw new ApplicationException("Informe um período válido com data inicial e final.");
		if (page == null)
			page = PageQuery.defaults();
	}
}
