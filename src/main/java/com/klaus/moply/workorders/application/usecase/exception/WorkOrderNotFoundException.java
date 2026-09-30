package com.klaus.moply.workorders.application.usecase.exception;

import java.util.UUID;

public class WorkOrderNotFoundException extends RuntimeException {

	public WorkOrderNotFoundException(UUID id) {
		super("Trabalho não encontrado: " + id);
	}

}
