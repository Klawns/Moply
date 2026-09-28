package com.klaus.moply.customers.domain.vo;

import com.klaus.moply.shared.domain.exception.DomainException;

public record Email(String value) {
	public Email {
		if (value == null || value.isBlank()) {
			throw new DomainException("O e-mail não pode ser nulo ou estar em branco.");
		}
		value = value.strip();
		int at = value.indexOf('@');
		if (at <= 0 || at == value.length() - 1 || at != value.lastIndexOf('@')
				|| value.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
			throw new DomainException("O e-mail deve conter um único @, partes não vazias e nenhum espaço em branco.");
		}
	}

	public static Email ofNullable(String value) {
		return value == null || value.isBlank() ? null : new Email(value);
	}
}
