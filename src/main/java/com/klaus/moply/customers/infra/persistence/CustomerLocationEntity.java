package com.klaus.moply.customers.infra.persistence;

import java.util.UUID;

import com.klaus.moply.customers.domain.entities.CustomerLocation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_customer_location")
@Getter
@NoArgsConstructor
public class CustomerLocationEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@jakarta.persistence.JoinColumns({
			@JoinColumn(name = "organization_id", referencedColumnName = "organization_id", nullable = false,
					insertable = false, updatable = false),
			@JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false, insertable = false,
					updatable = false) })
	private CustomerEntity customer;

	@Column(name = "customer_id", nullable = false, updatable = false)
	private UUID customerId;

	@Column(nullable = false, columnDefinition = "text")
	private String name;

	@Column(columnDefinition = "text")
	private String address;

	@Column(columnDefinition = "text")
	private String notes;

	public CustomerLocationEntity(UUID id, CustomerEntity customer) {
		this.id = id;
		this.customer = customer;
		this.customerId = customer.getId();
		this.organizationId = customer.getOrganizationId();
	}

	public void update(CustomerLocation location) {
		name = location.getName();
		address = location.getAddress();
		notes = location.getNotes();
	}

	public CustomerLocation toDomain() {
		return CustomerLocation.restore(id, name, address, notes);
	}

}
