package com.klaus.moply.orderservice.domain.vo;

import com.klaus.moply.orderservice.domain.exception.DomainException;

public record Customer(String name) {
	public Customer {
		if (name == null || name.isBlank()) {
			throw new DomainException("Nome cant be empty or null");
		}
	}
}
