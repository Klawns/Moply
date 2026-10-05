package com.klaus.moply.workorders.domain.vo;

public record WorkOrderDescription(String value) {
	public WorkOrderDescription {
		value = value == null || value.isBlank() ? null : value.strip();
	}
}
