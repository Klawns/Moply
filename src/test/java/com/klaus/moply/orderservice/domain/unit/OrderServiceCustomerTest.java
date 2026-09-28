package com.klaus.moply.orderservice.domain.unit;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.factory.OrderServiceFactory;
import com.klaus.moply.orderservice.domain.entity.OrderService;
import com.klaus.moply.shared.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceCustomerTest {

	@Test
	void shouldRequireRegisteredCustomer() {
		assertThrows(DomainException.class, () -> OrderServiceFactory.createOrderService(null));
		assertThrows(DomainException.class, () -> OrderServiceFactory.createOrderService(Customer.create("Maria")));
	}

	@Test
	void shouldChangeCustomerAndRejectInvalidChanges() {
		var order = OrderServiceFactory.createOrderService();
		var customer = Customer.restore(UUID.randomUUID(), "Outro nome");
		order.changeCustomer(customer);
		assertSame(customer, order.getCustomer());
		assertEquals("Outro nome", order.getCustomer().getName().value());
		assertThrows(DomainException.class, () -> order.changeCustomer(Customer.create("Sem ID")));
		assertThrows(DomainException.class, () -> order.changeCustomer(null));
		assertSame(customer, order.getCustomer());
	}

	@Test
	void shouldRestoreWithRegisteredCustomer() {
		var original = OrderServiceFactory.createOrderService();
		var customer = Customer.restore(UUID.randomUUID(), "Atual");
		var order = OrderService.restore(UUID.randomUUID(), customer, original.getContractedHours().value(),
				original.getHourlyRate().value(), original.getEmployeeCount(), original.getServiceDate());
		assertEquals("Atual", order.getCustomer().getName().value());
		assertSame(customer, order.getCustomer());
	}

}
