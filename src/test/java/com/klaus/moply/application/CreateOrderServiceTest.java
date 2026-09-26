package com.klaus.moply.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.klaus.moply.application.ports.OrderServiceRepository;
import com.klaus.moply.application.usecase.CreateOrderService;
import com.klaus.moply.application.usecase.dto.CreateOrderServiceInput;
import com.klaus.moply.domain.entity.OrderService;

public class CreateOrderServiceTest {

	private OrderServiceRepository repo;

	private CreateOrderService useCase;

	@BeforeEach
	void setUp() {
		repo = mock(OrderServiceRepository.class);

		useCase = new CreateOrderService(repo);
	}

	@Test
	@DisplayName("Deve criar uma prestação.")
	void shouldCreatePrestacaoServico() {
		CreateOrderServiceInput command = new CreateOrderServiceInput("João", new BigDecimal("4.00"),
				new BigDecimal("11.50"), 2, LocalDate.of(2026, 9, 21));

		UUID idGerado = UUID.randomUUID();

		when(repo.save(any(OrderService.class))).thenAnswer(invocation -> {
			OrderService dominioPassado = invocation.getArgument(0);
			return OrderService.restore(idGerado, dominioPassado.getCustomer().name(),
					dominioPassado.getContractedHours().value(), dominioPassado.getHourlyRate().value(),
					dominioPassado.getEmployeeCount(), dominioPassado.getServiceDate());
		});

		UUID id = useCase.execute(command);

		assertEquals(idGerado, id);

		verify(repo).save(any(OrderService.class));
	}

}
