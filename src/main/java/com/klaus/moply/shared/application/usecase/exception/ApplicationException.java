package com.klaus.moply.shared.application.usecase.exception;

import com.klaus.moply.shared.exception.ErrorCategory;
import com.klaus.moply.shared.exception.CoreException;

public class ApplicationException extends CoreException {

	public ApplicationException(String message) {
		this("APPLICATION_ERROR", message);
	}

	public ApplicationException(String code, String message) {
		super(ErrorCategory.APPLICATION_ERROR, code, message);
	}

	protected ApplicationException(String code, String message, Throwable cause) {
		super(ErrorCategory.APPLICATION_ERROR, code, message, cause);
	}

}
