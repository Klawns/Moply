package com.klaus.moply.orderservice.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.application.usecase.CreateOrderService;
import com.klaus.moply.orderservice.application.usecase.DeleteOrderService;
import com.klaus.moply.orderservice.application.usecase.FindAllOrderServicesByCustomerName;
import com.klaus.moply.orderservice.application.usecase.FindOrderServiceById;
import com.klaus.moply.orderservice.application.usecase.FindOrderServiceByServiceDate;

@Configuration
public class OrderServiceConfig {

	@Bean
	public FindOrderServiceByServiceDate findOrderServiceByServiceDate(OrderServiceRepository repository) {
		return new FindOrderServiceByServiceDate(repository);
	}

	@Bean
	public FindAllOrderServicesByCustomerName findAllOrderServicesByCustomerName(OrderServiceRepository repository) {
		return new FindAllOrderServicesByCustomerName(repository);
	}

	@Bean
	public FindOrderServiceById findOrderServiceById(OrderServiceRepository repository) {
		return new FindOrderServiceById(repository);
	}

	@Bean
	public CreateOrderService createOrderService(OrderServiceRepository repository, CustomerRepository customers) {
		return new CreateOrderService(repository, customers);
	}

	@Bean
	public DeleteOrderService deleteOrderService(OrderServiceRepository repository) {
		return new DeleteOrderService(repository);
	}

}
