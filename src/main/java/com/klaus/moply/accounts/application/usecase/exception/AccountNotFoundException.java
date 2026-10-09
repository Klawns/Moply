package com.klaus.moply.accounts.application.usecase.exception;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class AccountNotFoundException extends ApplicationException {

	public AccountNotFoundException() {
		super("ACCOUNT_NOT_FOUND", "Conta não encontrada.");
	}

}
