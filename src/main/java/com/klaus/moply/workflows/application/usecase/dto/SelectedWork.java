package com.klaus.moply.workflows.application.usecase.dto;

import com.klaus.moply.workorders.domain.entity.WorkOrder;

public record SelectedWork(WorkOrder work, long position) {
}
