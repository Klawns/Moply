package com.klaus.moply.accounts.infra.web.dto;

public record RegistrationRequest(String name, String timezone, String email, String password) {
	@Override
	public String toString() {
		return "RegistrationRequest[redacted]";
	}
}
