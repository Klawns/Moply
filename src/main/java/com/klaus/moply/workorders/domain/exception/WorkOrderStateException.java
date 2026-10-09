package com.klaus.moply.workorders.domain.exception;

import com.klaus.moply.shared.domain.exception.DomainException;

public class WorkOrderStateException extends DomainException {

	public WorkOrderStateException(String message) {
		super("WORK_ORDER_STATE_ERROR", message);
	}

}
