package com.klaus.moply.workorders.application.usecase.exception;

import java.util.UUID;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class WorkOrderNotFoundException extends ApplicationException {

	public WorkOrderNotFoundException(UUID id) {
		super("WORK_ORDER_NOT_FOUND", "Trabalho não encontrado: " + id);
	}

}
