package com.klaus.moply.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.application.ports.OrderServiceRepository;
import com.klaus.moply.application.usecase.CreateOrderService;
import com.klaus.moply.application.usecase.DeleteOrderService;
import com.klaus.moply.application.usecase.FindAllOrderServicesByCustomerName;
import com.klaus.moply.application.usecase.FindOrderServiceById;
import com.klaus.moply.application.usecase.FindOrderServiceByServiceDate;

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
	public CreateOrderService createOrderService(OrderServiceRepository repository) {
		return new CreateOrderService(repository);
	}

	@Bean
	public DeleteOrderService deleteOrderService(OrderServiceRepository repository) {
		return new DeleteOrderService(repository);
	}

}
