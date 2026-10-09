package com.klaus.moply.accounts.application.usecase.dto;

public record RegisterAccountInput(String name, String timezone, String email, String password) {

	@Override
	public String toString() {
		return "RegisterAccount.Input[redacted]";
	}
}
