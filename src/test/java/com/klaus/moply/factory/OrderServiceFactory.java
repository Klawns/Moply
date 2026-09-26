package com.klaus.moply.factory;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.klaus.moply.domain.entity.OrderService;

public class OrderServiceFactory {

	public static OrderService createOrderService() {
		return OrderService.create("Cliente Teste", new BigDecimal(4.00), new BigDecimal(11.50), 2,
				LocalDate.of(2026, 9, 18));
	}

}
