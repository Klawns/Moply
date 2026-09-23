package com.klaus.moply.domain.vo;

import com.klaus.moply.domain.exception.DomainException;

public record Customer(String name) {
    public Customer {
        if (name.isBlank() || name == null) {
            throw new DomainException("Nome cant be empty or null");
        }
    }
}
