package com.klaus.moply.customers.infra.web;

import static com.klaus.moply.factory.AccountFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
// Characterizes the existing controller/persistence contract; security is covered by
// AccountApiIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CustomerApiIntegrationTest {

	@org.junit.jupiter.api.BeforeEach
	void authenticateFixture() {
		authenticate();
		jdbc.update(
				"MERGE INTO tb_organization (id,name,currency_code,timezone,default_work_status) KEY(id) VALUES (?, 'Test', 'GBP', 'UTC', 'SCHEDULED')",
				ACCOUNT);
	}

	@org.junit.jupiter.api.AfterEach
	void clearAuthentication() {
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
	}

	@Autowired
	com.klaus.moply.collaborators.application.ports.CollaboratorRepository collaborators;

	@Autowired
	org.springframework.jdbc.core.JdbcTemplate jdbc;

	private UUID participant() {
		return collaborators
			.save(ACCOUNT, com.klaus.moply.collaborators.domain.entities.Collaborator.create(ACCOUNT, "Worker", null))
			.getId();
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	CustomerRepository customers;

	@Autowired
	WorkOrderRepository orders;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	EntityManager entityManager;

	@AfterEach
	void cleanUp() {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			entityManager.createQuery("delete from WorkAssignmentEntity").executeUpdate();
			entityManager.createQuery("delete from WorkOrderEntity").executeUpdate();
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
			.getHeader("Location")
			.replaceAll(".*/", "");
	}

	@Test
	void shouldManageCustomersLocationsAndShowCurrentCustomerNameInOrders() throws Exception {
		String customerId = createCustomer();
		UUID initialLocationId = customers.findById(ACCOUNT, UUID.fromString(customerId))
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
			.andExpect(jsonPath("$.content[0].id").value(customerId))
			.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/customers").param("page", "1").param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1));
		mvc.perform(get("/api/v1/customers").param("size", "101")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/customers").param("sort", "notes")).andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/customers/" + customerId + "/locations").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"Escritório","address":"Rua A"}
					""")).andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Escritório"));
		mvc.perform(put("/api/v1/customers/" + customerId + "/locations/" + initialLocationId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"Casa nova","address":"Rua B","notes":"Portaria"}
					""")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(initialLocationId.toString()));
		mvc.perform(get("/api/v1/customers/" + customerId + "/locations/" + initialLocationId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.address").value("Rua B"));
		mvc.perform(get("/api/v1/customers/" + customerId + "/locations"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.totalPages").value(1));
		mvc.perform(get("/api/v1/customers/" + customerId + "/locations").param("page", "1").param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.totalPages").value(2));
		String orderId = mvc
			.perform(post("/api/v1/work-orders").contentType(MediaType.APPLICATION_JSON)
				.content(
						"""
								{"customerId":"%s","contractedHours":4,"hourlyRate":10,"participantIds":["%s"],"serviceDate":"2026-09-28"}
								"""
							.formatted(customerId, participant())))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getHeader("Location")
			.replaceAll(".*/", "");
		mvc.perform(put("/api/v1/customers/" + customerId).contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"Maria Silva","phone":"456"}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.locations.length()").value(2));
		mvc.perform(get("/api/v1/work-orders/" + orderId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.customerId").value(customerId))
			.andExpect(jsonPath("$.customer").value("Maria Silva"));
		var loaded = orders.findById(ACCOUNT, UUID.fromString(orderId)).orElseThrow();
		mvc.perform(get("/api/v1/work-orders").param("from", "2026-09-28")
			.param("to", "2026-09-28")
			.param("customerId", UUID.randomUUID().toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/v1/work-orders").param("from", "2026-09-28").param("to", "2026-09-28"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].customer").value("Maria Silva"));
		assertEquals(UUID.fromString(customerId), loaded.customerId());
		assertEquals(1, loaded.participantCount());
		mvc.perform(get("/api/v1/work-orders").param("from", "2026-09-28")
			.param("to", "2026-09-28")
			.param("customerId", customerId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(orderId));
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
		UUID locationId = customers.findById(ACCOUNT, UUID.fromString(ownerId))
			.orElseThrow()
			.getLocations()
			.getFirst()
			.getId();
		UUID otherId = customers.save(ACCOUNT, Customer.create("Outro")).getId();
		mvc.perform(get("/api/v1/customers/" + otherId + "/locations/" + locationId)).andExpect(status().isNotFound());
		mvc.perform(
				put("/api/v1/customers/" + otherId + "/locations/" + locationId).contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Alterado\"}"))
			.andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/work-orders").contentType(MediaType.APPLICATION_JSON)
			.content(
					"""
							{"customerId":"%s","contractedHours":4,"hourlyRate":10,"participantIds":["%s"],"serviceDate":"2026-09-28"}
							"""
						.formatted(missing, participant())))
			.andExpect(status().isNotFound());
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
		mvc.perform(post("/api/v1/work-orders").contentType(MediaType.APPLICATION_JSON)
			.content(
					"""
							{"customerId":"invalid","contractedHours":4,"hourlyRate":10,"participantIds":["%s"],"serviceDate":"2026-09-28"}
							"""
						.formatted(participant())))
			.andExpect(status().isBadRequest());
		assertEquals(0,
				customers.findAll(ACCOUNT, new com.klaus.moply.shared.application.pagination.PageQuery(0, 20, null))
					.totalElements());
	}

	@Test
	void shouldCreateOrderForCustomerWithLongName() throws Exception {
		String name = "A".repeat(300);
		var customer = customers.save(ACCOUNT, Customer.create(name));
		String id = mvc
			.perform(post("/api/v1/work-orders").contentType(MediaType.APPLICATION_JSON)
				.content(
						"""
								{"customerId":"%s","contractedHours":4,"hourlyRate":10,"participantIds":["%s"],"serviceDate":"2026-09-28"}
								"""
							.formatted(customer.getId(), participant())))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getHeader("Location")
			.replaceAll(".*/", "");
		mvc.perform(get("/api/v1/work-orders/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.customer").value(name));
	}

}
