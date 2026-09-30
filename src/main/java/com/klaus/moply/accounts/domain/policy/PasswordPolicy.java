package com.klaus.moply.accounts.domain.policy;

import java.nio.charset.StandardCharsets;

import com.klaus.moply.shared.domain.exception.DomainException;

public final class PasswordPolicy {

	public static final int MINIMUM_CHARACTERS = 12;

	public static final int MAXIMUM_UTF8_BYTES = 72;

	private PasswordPolicy() {
	}

	public static void validate(String password) {
		if (password == null || password.isBlank()
				|| password.codePointCount(0, password.length()) < MINIMUM_CHARACTERS) {
			throw new DomainException("A senha deve ter pelo menos 12 caracteres.");
		}
		// Every UTF-16 code unit requires at least one UTF-8 byte. This avoids
		// allocating a second, potentially huge array for clearly oversized input.
		if (password.length() > MAXIMUM_UTF8_BYTES
				|| password.getBytes(StandardCharsets.UTF_8).length > MAXIMUM_UTF8_BYTES) {
			throw new DomainException("A senha deve ter no máximo 72 bytes em UTF-8.");
		}
	}

}
