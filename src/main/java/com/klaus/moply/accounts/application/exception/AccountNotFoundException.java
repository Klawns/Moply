package com.klaus.moply.accounts.application.exception;

public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException() {
		super("Conta não encontrada.");
	}

}
