package com.klaus.moply.reports.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public record CollaboratorsReportInput(ReportPeriod period, UUID collaboratorId) {
	public CollaboratorsReportInput {
		if (period == null)
			throw new ApplicationException("Informe o período.");
	}
}
