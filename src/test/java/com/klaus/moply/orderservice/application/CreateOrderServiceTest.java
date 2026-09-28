package com.klaus.moply.orderservice.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;
import com.klaus.moply.orderservice.application.usecase.CreateOrderService;
import com.klaus.moply.orderservice.application.usecase.dto.CreateOrderServiceInput;
import com.klaus.moply.orderservice.domain.entity.OrderService;
import com.klaus.moply.shared.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateOrderServiceTest {

	private OrderServiceRepository repo;

	private CreateOrderService useCase;

	private CustomerRepository customers;

	@BeforeEach
	void setUp() {
		repo = mock(OrderServiceRepository.class);

		customers = mock(CustomerRepository.class);
		useCase = new CreateOrderService(repo, customers);
	}

	@Test
	@DisplayName("Deve criar uma prestação.")
	void shouldCreatePrestacaoServico() {
		var customer = Customer.restore(UUID.randomUUID(), "João");
		when(customers.findById(customer.getId())).thenReturn(Optional.of(customer));
		CreateOrderServiceInput command = new CreateOrderServiceInput(customer.getId(), new BigDecimal("4.00"),
				new BigDecimal("11.50"), 2, LocalDate.of(2026, 9, 21));

		UUID idGerado = UUID.randomUUID();

		when(repo.save(any(OrderService.class))).thenAnswer(invocation -> {
			OrderService dominioPassado = invocation.getArgument(0);
			return OrderService.restore(idGerado, dominioPassado.getCustomer(),
					dominioPassado.getContractedHours().value(), dominioPassado.getHourlyRate().value(),
					dominioPassado.getEmployeeCount(), dominioPassado.getServiceDate());
		});

		UUID id = useCase.execute(command);

		assertEquals(idGerado, id);

		verify(repo).save(any(OrderService.class));
	}

	@Test
	void shouldRejectUnknownCustomerBeforeSaving() {
		UUID customerId = UUID.randomUUID();
		when(customers.findById(customerId)).thenReturn(Optional.empty());
		var input = new CreateOrderServiceInput(customerId, BigDecimal.ONE, BigDecimal.TEN, 1, LocalDate.now());
		assertThrows(CustomerNotFoundException.class, () -> useCase.execute(input));
		verifyNoInteractions(repo);
	}

	@Test
	void shouldRejectMissingCustomerId() {
		var input = new CreateOrderServiceInput(null, BigDecimal.ONE, BigDecimal.TEN, 1, LocalDate.now());
		assertThrows(DomainException.class, () -> useCase.execute(input));
		verifyNoInteractions(repo, customers);
	}

}
