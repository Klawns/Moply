package com.klaus.moply.orderservice.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.application.usecase.FindOrderServiceById;
import com.klaus.moply.orderservice.application.usecase.dto.OrderServiceOutput;
import com.klaus.moply.orderservice.domain.entity.OrderService;

public class FindOrderServiceByIdTest {

	private OrderServiceRepository repo;

	private FindOrderServiceById useCase;

	@BeforeEach
	void setUp() {
		repo = mock(OrderServiceRepository.class);

		useCase = new FindOrderServiceById(repo);
	}

	@Test
	void deveBuscarPrestacaoServicoPorId() {
		UUID id = UUID.randomUUID();

		OrderService orderService = OrderService.restore(id, "João", new BigDecimal("4.00"), new BigDecimal("11.50"), 2,
				LocalDate.of(2026, 9, 21));

		when(repo.findById(id)).thenReturn(Optional.of(orderService));

		OrderServiceOutput result = useCase.execute(id);

		assertNotNull(result);

		assertEquals(id, result.id());
		assertEquals("João", result.customer());
		assertEquals(new BigDecimal("4.00"), result.contractedHours());
		assertEquals(new BigDecimal("11.50"), result.hourlyPrice());
		assertEquals(2, result.employeeCount());
		assertEquals(LocalDate.of(2026, 9, 21), result.serviceDate());

		assertEquals(new BigDecimal("46.00"), result.totalAmount());
		assertEquals(new BigDecimal("2.00"), result.individualHour());
		assertEquals(new BigDecimal("23.00"), result.individualAmount());

		verify(repo).findById(id);
	}

}
