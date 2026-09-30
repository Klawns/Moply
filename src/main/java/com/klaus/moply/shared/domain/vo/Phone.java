package com.klaus.moply.shared.domain.vo;

import com.klaus.moply.shared.domain.exception.DomainException;

public record Phone(String value) {
	public Phone {
		if (value == null || value.isBlank()) {
			throw new DomainException("O telefone não pode ser nulo ou estar em branco.");
		}
		value = value.strip();
	}

	public static Phone ofNullable(String value) {
		return value == null || value.isBlank() ? null : new Phone(value);
	}
}
