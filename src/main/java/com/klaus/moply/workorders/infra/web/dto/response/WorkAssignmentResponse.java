package com.klaus.moply.workorders.infra.web.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput.AssignmentOutput;
import com.klaus.moply.workorders.domain.entity.WorkAssignment;

public record WorkAssignmentResponse(UUID collaboratorId, int inclusionPosition, BigDecimal allocatedAmount,
		BigDecimal appliedHourlyRate, Boolean fixedRate, BigDecimal baseAmount, BigDecimal surplusAmount) {

	public static WorkAssignmentResponse from(AssignmentOutput a) {
		return new WorkAssignmentResponse(a.collaboratorId(), a.inclusionPosition(), a.allocatedAmount(),
				a.appliedHourlyRate(), a.fixedRate(), a.baseAmount(), a.surplusAmount());
	}

	public static WorkAssignmentResponse from(WorkAssignment a) {
		return new WorkAssignmentResponse(a.collaboratorId(), a.inclusionPosition(), a.allocatedAmount().value(),
				a.appliedHourlyRate() == null ? null : a.appliedHourlyRate().value(), a.fixedRate(),
				a.baseAmount() == null ? null : a.baseAmount().value(),
				a.surplusAmount() == null ? null : a.surplusAmount().value());
	}

}
