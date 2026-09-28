package com.klaus.moply.customers.application.ports;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import com.klaus.moply.customers.domain.Customer;

public interface CustomerRepository {

	List<Customer> findAll();

	Customer save(Customer customer);

	Optional<Customer> findById(UUID id);

}
