package com.klaus.moply.customers.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.customers.domain.entities.Customer;

public interface CustomerRepository {

	List<Customer> findAll(UUID organizationId);

	Customer save(UUID organizationId, Customer customer);

	Optional<Customer> findById(UUID organizationId, UUID id);

}
