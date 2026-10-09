package com.klaus.moply.shared.domain.exception;

import com.klaus.moply.shared.exception.ErrorCategory;
import com.klaus.moply.shared.exception.CoreException;

public class DomainException extends CoreException {

	public DomainException(String msg) {
		this("DOMAIN_ERROR", msg);
	}

	public DomainException(String code, String msg) {
		super(ErrorCategory.DOMAIN_ERROR, code, msg);
	}

}
