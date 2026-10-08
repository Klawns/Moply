package com.klaus.moply.customers.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.AddCustomerLocation;
import com.klaus.moply.customers.application.usecase.CreateCustomer;
import com.klaus.moply.customers.application.usecase.FindAllCustomers;
import com.klaus.moply.customers.application.usecase.FindCustomerById;
import com.klaus.moply.customers.application.usecase.FindCustomerLocationById;
import com.klaus.moply.customers.application.usecase.FindCustomerLocations;
import com.klaus.moply.customers.application.usecase.UpdateCustomer;
import com.klaus.moply.customers.application.usecase.UpdateCustomerLocation;

@Configuration
public class CustomerConfig {

	@Bean
	public com.klaus.moply.customers.application.usecase.FindAllLocations findAllLocations(
			com.klaus.moply.customers.application.ports.LocationReadRepository repo) {
		return new com.klaus.moply.customers.application.usecase.FindAllLocations(repo);
	}

	@Bean
	public CreateCustomer createCustomer(CustomerRepository repo) {
		return new CreateCustomer(repo);
	}

	@Bean
	public FindAllCustomers findAllCustomers(CustomerRepository repo) {
		return new FindAllCustomers(repo);
	}

	@Bean
	public FindCustomerById findCustomerById(CustomerRepository repo) {
		return new FindCustomerById(repo);
	}

	@Bean
	public UpdateCustomer updateCustomer(CustomerRepository repo) {
		return new UpdateCustomer(repo);
	}

	@Bean
	public AddCustomerLocation addCustomerLocation(CustomerRepository repo) {
		return new AddCustomerLocation(repo);
	}

	@Bean
	public FindCustomerLocations findCustomerLocations(CustomerRepository repo) {
		return new FindCustomerLocations(repo);
	}

	@Bean
	public FindCustomerLocationById findCustomerLocationById(CustomerRepository repo) {
		return new FindCustomerLocationById(repo);
	}

	@Bean
	public UpdateCustomerLocation updateCustomerLocation(CustomerRepository repo) {
		return new UpdateCustomerLocation(repo);
	}

}
