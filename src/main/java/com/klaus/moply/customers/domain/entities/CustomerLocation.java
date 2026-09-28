package com.klaus.moply.customers.domain.entities;

import java.util.UUID;
import com.klaus.moply.domain.exception.DomainException;
import lombok.Getter;

@Getter
public final class CustomerLocation {

	private final UUID id;

	private final String name;

	private final String address;

	private final String notes;

	private CustomerLocation(UUID id, String name, String address, String notes) {
		if (id == null) {
			throw new DomainException("O ID do local é obrigatório.");
		}
		if (name == null || name.isBlank()) {
			throw new DomainException("O nome do local é obrigatório.");
		}
		this.id = id;
		this.name = name.strip();
		this.address = normalizeOptional(address);
		this.notes = normalizeOptional(notes);
	}

	public static CustomerLocation create(String name, String address, String notes) {
		return new CustomerLocation(UUID.randomUUID(), name, address, notes);
	}

	public static CustomerLocation restore(UUID id, String name, String address, String notes) {
		return new CustomerLocation(id, name, address, notes);
	}

	public CustomerLocation update(String name, String address, String notes) {
		return new CustomerLocation(id, name, address, notes);
	}

	private static String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
