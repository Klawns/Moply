package com.klaus.moply.customers.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.domain.exception.DomainException;
import com.klaus.moply.customers.domain.vo.CustomerName;
import com.klaus.moply.customers.domain.vo.Phone;
import com.klaus.moply.customers.domain.vo.Email;

import lombok.Getter;

@Getter
public final class Customer {

	private final UUID id;

	private final CustomerName name;

	private final Phone phone;

	private final Email email;

	private final String notes;

	private final List<CustomerLocation> locations;

	private Customer(UUID id, CustomerName name, Phone phone, Email email, String notes,
			List<CustomerLocation> locations) {
		this.id = id;
		this.name = name;
		this.phone = phone;
		this.email = email;
		this.notes = normalizeNotes(notes);
		this.locations = validateAndCopyLocations(locations);
	}

	public static Customer create(String name) {
		return create(name, null, null, null);
	}

	public static Customer create(String name, String phone, String email, String notes) {
		return create(name, phone, email, notes, List.of());
	}

	public static Customer restore(UUID id, String name) {
		return restore(id, name, null, null, null);
	}

	public static Customer restore(UUID id, String name, String phone, String email, String notes) {
		return restore(id, name, phone, email, notes, List.of());
	}

	public Customer update(String name, String phone, String email, String notes) {
		return new Customer(id, new CustomerName(name), Phone.ofNullable(phone), Email.ofNullable(email), notes,
				locations);
	}

	public static Customer create(String name, String phone, String email, String notes,
			List<CustomerLocation> locations) {
		return new Customer(null, new CustomerName(name), Phone.ofNullable(phone), Email.ofNullable(email), notes,
				locations);
	}

	public static Customer restore(UUID id, String name, String phone, String email, String notes,
			List<CustomerLocation> locations) {
		if (id == null) {
			throw new DomainException("O ID do cliente é obrigatório para reconstituição.");
		}
		return new Customer(id, new CustomerName(name), Phone.ofNullable(phone), Email.ofNullable(email), notes,
				locations);
	}

	public Customer addLocation(CustomerLocation location) {
		if (location == null) {
			throw new DomainException("O local não pode ser nulo.");
		}
		var updated = new ArrayList<>(locations);
		updated.add(location);
		return new Customer(id, name, phone, email, notes, updated);
	}

	public CustomerLocation findLocation(UUID locationId) {
		if (locationId == null) {
			throw new DomainException("O ID do local é obrigatório.");
		}
		return locations.stream()
			.filter(location -> location.getId().equals(locationId))
			.findFirst()
			.orElseThrow(() -> new CustomerLocationNotFoundException(locationId));
	}

	public Customer updateLocation(UUID locationId, String name, String address, String notes) {
		CustomerLocation updated = findLocation(locationId).update(name, address, notes);
		var updatedLocations = locations.stream()
			.map(location -> location.getId().equals(locationId) ? updated : location)
			.toList();
		return new Customer(id, this.name, phone, email, this.notes, updatedLocations);
	}

	private static List<CustomerLocation> validateAndCopyLocations(List<CustomerLocation> locations) {
		if (locations == null || locations.stream().anyMatch(location -> location == null)) {
			throw new DomainException("A lista de locais não pode ser nula nem conter itens nulos.");
		}
		var ids = new HashSet<UUID>();
		if (locations.stream().anyMatch(location -> !ids.add(location.getId()))) {
			throw new DomainException("Os IDs dos locais devem ser únicos dentro do cliente.");
		}
		return List.copyOf(locations);
	}

	private static String normalizeNotes(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
