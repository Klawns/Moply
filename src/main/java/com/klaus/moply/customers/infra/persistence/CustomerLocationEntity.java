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

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private CustomerEntity customer;

	@Column(nullable = false, columnDefinition = "text")
	private String name;

	@Column(columnDefinition = "text")
	private String address;

	@Column(columnDefinition = "text")
	private String notes;

	public CustomerLocationEntity(UUID id, CustomerEntity customer) {
		this.id = id;
		this.customer = customer;
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
