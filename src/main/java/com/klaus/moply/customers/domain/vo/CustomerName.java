package com.klaus.moply.customers.domain.vo;

import com.klaus.moply.domain.exception.DomainException;

public record CustomerName(String value) {
	public CustomerName {
		if (value == null || value.isBlank()) {
			throw new DomainException("O nome do cliente é obrigatório.");
		}
		value = value.strip();
	}
}
