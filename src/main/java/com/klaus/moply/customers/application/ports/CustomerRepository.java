package com.klaus.moply.customers.application.ports;

import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;

public interface CustomerRepository {

	PageResult<Customer> findAll(UUID organizationId, PageQuery page);

	PageResult<CustomerLocation> findLocations(UUID organizationId, UUID customerId, PageQuery page);

	Customer save(UUID organizationId, Customer customer);

	Optional<Customer> findById(UUID organizationId, UUID id);

}
