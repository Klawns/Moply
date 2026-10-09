package com.klaus.moply.accounts.application.usecase.exception;

public class AccountConflictException extends RuntimeException {

	public AccountConflictException() {
		super("Cadastro de conta em conflito.");
	}

}
