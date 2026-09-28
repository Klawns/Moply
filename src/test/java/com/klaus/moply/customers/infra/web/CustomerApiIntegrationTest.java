package com.klaus.moply.customers.infra.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.orderservice.application.ports.OrderServiceRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerApiIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	CustomerRepository customers;

	@Autowired
	OrderServiceRepository orders;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	EntityManager entityManager;

	@AfterEach
	void cleanUp() {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			entityManager.createQuery("delete from OrderServiceEntity").executeUpdate();
			entityManager.createQuery("delete from CustomerLocationEntity").executeUpdate();
			entityManager.createQuery("delete from CustomerEntity").executeUpdate();
		});
	}

	private String createCustomer() throws Exception {
		return mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"  Maria  ","phone":"123","email":"a@b","locations":[{"name":"Casa"}]}
				"""))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andReturn()
				.getResponse()
				.getContentAsString()
				.replace("\"", "");
	}

	@Test
	void shouldManageCustomersLocationsAndShowCurrentCustomerNameInOrders() throws Exception {
		String customerId = createCustomer();
		UUID initialLocationId = customers.findById(UUID.fromString(customerId))
				.orElseThrow()
				.getLocations()
				.getFirst()
				.getId();
		mvc.perform(get("/api/v1/customers/" + customerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Maria"))
				.andExpect(jsonPath("$.locations[0].id").value(initialLocationId.toString()));
		mvc.perform(get("/api/v1/customers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(customerId));
		mvc.perform(post("/api/v1/customers/" + customerId + "/locations").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Escritório","address":"Rua A"}
						""")).andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Escritório"));
		mvc.perform(put("/api/v1/customers/" + customerId + "/locations/" + initialLocationId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Casa nova","address":"Rua B","notes":"Portaria"}
						""")).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(initialLocationId.toString()));
		mvc.perform(get("/api/v1/customers/" + customerId + "/locations/" + initialLocationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address").value("Rua B"));
		mvc.perform(get("/api/v1/customers/" + customerId + "/locations"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
		String orderId = mvc.perform(post("/api/v1/order-services").contentType(MediaType.APPLICATION_JSON).content("""
				{"customerId":"%s","contractedHours":4,"hourlyRate":10,"employeeCount":2,"serviceDate":"2026-09-28"}
				""".formatted(customerId)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString()
				.replace("\"", "");
		mvc.perform(put("/api/v1/customers/" + customerId).contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"Maria Silva","phone":"456"}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.locations.length()").value(2));
		mvc.perform(get("/api/v1/order-services/" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerId").value(customerId))
				.andExpect(jsonPath("$.customer").value("Maria Silva"));
		var loaded = orders.findById(UUID.fromString(orderId)).orElseThrow();
		mvc.perform(get("/api/v1/order-services").param("serviceDate", "2026-09-28").param("customer", "Maria"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/v1/order-services").param("serviceDate", "2026-09-28"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].customer").value("Maria Silva"));
		assertEquals("Maria Silva", loaded.getCustomer().getName().value());
		assertEquals(2, loaded.getCustomer().getLocations().size());
		mvc.perform(get("/api/v1/order-services").param("serviceDate", "2026-09-28").param("customer", "Maria Silva"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(orderId));
	}

	@Test
	void shouldReturnNotFoundForUnknownCustomerAndForeignLocation() throws Exception {
		UUID missing = UUID.randomUUID();
		mvc.perform(get("/api/v1/customers/" + missing)).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/customers/" + missing + "/locations")).andExpect(status().isNotFound());
		mvc.perform(put("/api/v1/customers/" + missing).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Nome\"}")).andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/customers/" + missing + "/locations").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Casa\"}")).andExpect(status().isNotFound());
		String ownerId = createCustomer();
		UUID locationId = customers.findById(UUID.fromString(ownerId)).orElseThrow().getLocations().getFirst().getId();
		UUID otherId = customers.save(Customer.create("Outro")).getId();
		mvc.perform(get("/api/v1/customers/" + otherId + "/locations/" + locationId)).andExpect(status().isNotFound());
		mvc.perform(
				put("/api/v1/customers/" + otherId + "/locations/" + locationId).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Alterado\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/order-services").contentType(MediaType.APPLICATION_JSON).content("""
				{"customerId":"%s","contractedHours":4,"hourlyRate":10,"employeeCount":2,"serviceDate":"2026-09-28"}
				""".formatted(missing))).andExpect(status().isNotFound());
	}

	@Test
	void shouldRejectInvalidRequests() throws Exception {
		for (String body : List.of("{\"name\":\" \"}", "{\"name\":\"Maria\",\"email\":\"invalid\"}",
				"{\"name\":\"Maria\",\"locations\":[null]}", "{\"name\":\"Maria\",\"locations\":[{\"name\":\"\"}]}",
				"{")) {
			mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isBadRequest());
		}
		mvc.perform(get("/api/v1/customers/not-a-uuid")).andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/order-services").contentType(MediaType.APPLICATION_JSON)
				.content(
						"""
								{"customerId":"invalid","contractedHours":4,"hourlyRate":10,"employeeCount":2,"serviceDate":"2026-09-28"}
								"""))
				.andExpect(status().isBadRequest());
		assertTrue(customers.findAll().isEmpty());
	}

	@Test
	void shouldCreateOrderForCustomerWithLongName() throws Exception {
		String name = "A".repeat(300);
		var customer = customers.save(Customer.create(name));
		String id = mvc.perform(post("/api/v1/order-services").contentType(MediaType.APPLICATION_JSON).content("""
				{"customerId":"%s","contractedHours":4,"hourlyRate":10,"employeeCount":2,"serviceDate":"2026-09-28"}
				""".formatted(customer.getId())))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString()
				.replace("\"", "");
		mvc.perform(get("/api/v1/order-services/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customer").value(name));
	}

}
