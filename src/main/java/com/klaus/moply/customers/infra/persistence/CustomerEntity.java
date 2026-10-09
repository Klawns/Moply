package com.klaus.moply.customers.infra.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.klaus.moply.customers.domain.entities.Customer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_customer",
		uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = { "organization_id", "id" }))
@Getter
@NoArgsConstructor
public class CustomerEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@jakarta.persistence.Version
	private long version;

	@Column(nullable = false, columnDefinition = "text")
	private String name;

	@Column(columnDefinition = "text")
	private String phone;

	@Column(columnDefinition = "text")
	private String email;

	@Column(columnDefinition = "text")
	private String notes;

	@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CustomerLocationEntity> locations = new ArrayList<>();

	public CustomerEntity(UUID id, UUID organizationId) {
		this.organizationId = organizationId;
		this.id = id;
	}

	public void update(Customer customer) {
		name = customer.getName().value();
		phone = customer.getPhone() == null ? null : customer.getPhone().value();
		email = customer.getEmail() == null ? null : customer.getEmail().value();
		notes = customer.getNotes();
		var retainedIds = customer.getLocations().stream().map(location -> location.getId()).toList();
		locations.removeIf(location -> !retainedIds.contains(location.getId()));
		for (var location : customer.getLocations()) {
			var entity = locations.stream()
				.filter(existing -> existing.getId().equals(location.getId()))
				.findFirst()
				.orElse(null);
			if (entity == null) {
				entity = new CustomerLocationEntity(location.getId(), this);
				locations.add(entity);
			}
			entity.update(location);
		}
	}

	public Customer toDomain() {
		return Customer
			.restore(id, name, phone, email, notes,
					locations.stream().map(entity -> Objects.requireNonNull(entity).toDomain()).toList())
			.withPersistence(organizationId, version);
	}

}
