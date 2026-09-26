package com.klaus.moply.infra.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.klaus.moply.application.ports.OrderServiceRepository;
import com.klaus.moply.domain.entity.OrderService;
import com.klaus.moply.factory.OrderServiceFactory;

@DataJpaTest
@ActiveProfiles("test")
@Import(OrderServiceJpaRepositoryAdapter.class)
public class OrderServiceRepositoryIntegrationTest {

	@Autowired
	private OrderServiceRepository repo;

	@Test
	@DisplayName("Deve criar prestação de serviço corretamente e buscá-la.")
	void shouldSaveAndFindOrderService() {

		OrderService orderService = OrderServiceFactory.createOrderService();

		OrderService saved = repo.save(orderService);

		Optional<OrderService> result = repo.findById(saved.getId());

		assertTrue(result.isPresent());

		assertEquals("Cliente Teste", result.get().getCustomer().name());
		assertTrue(new BigDecimal("4.00").compareTo(result.get().getContractedHours().value()) == 0);
		assertTrue(new BigDecimal("11.50").compareTo(result.get().getHourlyRate().value()) == 0);
		assertEquals(LocalDate.of(2026, 9, 18), result.get().getServiceDate());
	}

	@Test
	@DisplayName("Deve atualizar o cliente de uma prestação de serviço existente em vez de duplicar.")
	void shouldUpdateOrderServiceInsteadOfDuplicating() {
		OrderService orderService = OrderServiceFactory.createOrderService();
		OrderService saved = repo.save(orderService);
		UUID originalId = saved.getId();

		saved.changeCustomer("Cliente Atualizado");

		OrderService updated = repo.save(saved);

		assertEquals(originalId, updated.getId(), "O ID deveria continuar o mesmo.");
		assertEquals("Cliente Atualizado", updated.getCustomer().name());

		Optional<OrderService> databaseOrder = repo.findById(originalId);
		assertTrue(databaseOrder.isPresent());
		assertEquals("Cliente Atualizado", databaseOrder.get().getCustomer().name());
	}

	@Test
	@DisplayName("Deve buscar prestações de serviço filtrando pelo nome do cliente.")
	void shouldFindOrderServiceByCustomer() {
		OrderService order1 = OrderServiceFactory.createOrderService();
		OrderService order2 = OrderServiceFactory.createOrderService();

		repo.save(order1);
		repo.save(order2);

		List<OrderService> results = repo.findByCustomerName("Cliente Teste");

		assertTrue(results.size() >= 2);
		assertTrue(results.stream().allMatch(s -> s.getCustomer().name().equals("Cliente Teste")));
	}

	@Test
	@DisplayName("Deve buscar todas as prestações de serviço agendadas para uma data específica.")
	void shouldFindAllOrderServiceByServiceDate() {
		OrderService orderService = OrderServiceFactory.createOrderService();
		repo.save(orderService);

		LocalDate serviceDate = LocalDate.of(2026, 9, 18);
		List<OrderService> results = repo.findAllByServiceDate(serviceDate);

		assertTrue(results.size() >= 1);
		assertEquals(serviceDate, results.get(0).getServiceDate());
	}

	@Test
	@DisplayName("Deve deletar corretamente uma prestação de serviço.")
	void shouldDeleteOrderService() {
		OrderService orderService = OrderServiceFactory.createOrderService();
		OrderService saved = repo.save(orderService);
		UUID id = saved.getId();

		assertTrue(repo.findById(id).isPresent());

		repo.deleteById(id);

		Optional<OrderService> deleted = repo.findById(id);
		assertTrue(deleted.isEmpty());
	}

}
