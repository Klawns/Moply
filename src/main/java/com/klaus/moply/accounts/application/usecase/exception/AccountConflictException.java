package com.klaus.moply.accounts.application.usecase.exception;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class AccountConflictException extends ApplicationException {

	public AccountConflictException() {
		super("ACCOUNT_CONFLICT", "Cadastro de conta em conflito.");
	}

}
