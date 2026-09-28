package com.klaus.moply.factory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.orderservice.domain.entity.OrderService;

public class OrderServiceFactory {

	public static OrderService restoreOrderService(UUID id) {
		return OrderService.restore(id, Customer.restore(UUID.randomUUID(), "Cliente Teste"), new BigDecimal(4.00),
				new BigDecimal(11.50), 2, LocalDate.of(2026, 9, 18));
	}

	public static OrderService createOrderService() {
		return createOrderService(Customer.restore(UUID.randomUUID(), "Cliente Teste"));
	}

	public static OrderService createOrderService(Customer customer) {
		return OrderService.create(customer, new BigDecimal(4.00), new BigDecimal(11.50), 2, LocalDate.of(2026, 9, 18));
	}

}
