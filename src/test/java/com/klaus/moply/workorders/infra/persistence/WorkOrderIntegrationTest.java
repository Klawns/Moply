package com.klaus.moply.workorders.infra.persistence;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.*;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.domain.entities.*;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.*;
import com.klaus.moply.workorders.domain.entity.*;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

@SpringBootTest(properties = { "spring.jpa.open-in-view=false", "spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class WorkOrderIntegrationTest {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-bookworm");

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry p) {
		p.add("spring.datasource.url", postgres::getJdbcUrl);
		p.add("spring.datasource.username", postgres::getUsername);
		p.add("spring.datasource.password", postgres::getPassword);
		p.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		p.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
	}

	@Autowired
	WorkOrderRepository orders;

	@Autowired
	CustomerRepository customers;

	@Autowired
	CollaboratorRepository collaborators;

	@Autowired
	OrganizationRepository accounts;

	@Autowired
	FindWorkOrderById find;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MockMvc mvc;

	UUID account, foreignAccount, customer, person, second, location;

	AccountPrincipal principal;

	@BeforeEach
	void setup() {
		account = account();
		foreignAccount = account();
		var c = customers.save(account,
				Customer.create("Original").addLocation(CustomerLocation.create("Casa", null, null)));
		customer = c.getId();
		location = c.getLocations().getFirst().getId();
		person = collaborators.save(account, Collaborator.create(account, "First", null)).getId();
		second = collaborators.save(account, Collaborator.create(account, "Second", null)).getId();
		principal = new AccountPrincipal(new AppUser(UUID.randomUUID(), account, new LoginEmail("a@b"), "unused"));
	}

	@AfterEach
	void cleanup() {
		jdbc.update("DELETE FROM tb_work_assignment");
		jdbc.update("DELETE FROM tb_order_service");
		jdbc.update("DELETE FROM tb_customer_location");
		jdbc.update("DELETE FROM tb_customer");
		jdbc.update("DELETE FROM tb_collaborator");
		jdbc.update("DELETE FROM tb_organization");
	}

	UUID account() {
		UUID id = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO tb_organization(id,name,currency_code,timezone,default_work_status) VALUES (?,'Test','GBP','UTC','SCHEDULED')",
				id);
		return id;
	}

	WorkOrder work(LocalDate date, UUID c, String hours, String rate) {
		return WorkOrder.create(c, null, date, null, null, new BigDecimal(hours), new BigDecimal(rate),
				List.of(second, person), WorkOrderStatus.SCHEDULED);
	}

	@Test
	void shouldRoundTripZeroAndLargeValuesWithoutRedistributionAndRetainHistory() {
		for (String hours : List.of("0.01", "123456789012345678901234567890.12")) {
			var saved = orders.save(account, work(LocalDate.now(), customer, hours, "1.00"));
			var loaded = orders.findById(account, saved.id()).orElseThrow();
			assertEquals(saved, loaded);
			assertEquals(List.of(second, person),
					loaded.assignments().stream().map(WorkAssignment::collaboratorId).toList());
			assertEquals(saved.totalAmount().value(),
					loaded.assignments()
						.stream()
						.map(a -> a.allocatedAmount().value())
						.reduce(BigDecimal.ZERO, BigDecimal::add));
			assertEquals(1, loaded.allocationPolicyVersion());
			assertEquals(0, loaded.version());
		}
		var saved = orders.save(account, work(LocalDate.now(), customer, "3", "11.50"));
		var c = customers.findById(account, customer).orElseThrow();
		customers.save(account, c.update("Renamed", null, null, null));
		collaborators.save(account, collaborators.findById(account, person).orElseThrow().deactivate());
		accounts.update(accounts.findById(account).orElseThrow().withPreferences("UTC", DefaultWorkStatus.COMPLETED));
		var output = find.execute(new Context(account), saved.id());
		assertEquals("Renamed", output.customer());
		assertEquals(WorkOrderStatus.SCHEDULED, output.status());
		assertEquals(saved, orders.findById(account, saved.id()).orElseThrow());
		// Reconstitution must not invoke the current policy, even for a historical
		// policy/version.
		jdbc.update("UPDATE tb_order_service SET allocation_policy_version=7 WHERE id=?", saved.id());
		jdbc.update(
				"UPDATE tb_work_assignment SET allocated_amount=CASE WHEN inclusion_position=0 THEN 0 ELSE 34.50 END WHERE work_order_id=?",
				saved.id());
		var historical = orders.findById(account, saved.id()).orElseThrow();
		assertEquals(7, historical.allocationPolicyVersion());
		assertEquals(new BigDecimal("0.00"), historical.assignments().getFirst().allocatedAmount().value());
	}

	@Test
	void shouldRollbackRootAndEarlierAssignmentsWhenLaterAssignmentFails() {
		jdbc.execute(
				"ALTER TABLE tb_work_assignment ADD CONSTRAINT test_reject_second CHECK (inclusion_position <> 1)");
		int roots = jdbc.queryForObject("SELECT count(*) FROM tb_order_service", Integer.class);
		int assignments = jdbc.queryForObject("SELECT count(*) FROM tb_work_assignment", Integer.class);
		try {
			assertThrows(DataIntegrityViolationException.class,
					() -> orders.save(account, work(LocalDate.now(), customer, "4", "10")));
			assertEquals(roots, jdbc.queryForObject("SELECT count(*) FROM tb_order_service", Integer.class));
			assertEquals(assignments, jdbc.queryForObject("SELECT count(*) FROM tb_work_assignment", Integer.class));
		}
		finally {
			jdbc.execute("ALTER TABLE tb_work_assignment DROP CONSTRAINT test_reject_second");
		}
	}

	@Test
	void shouldCombineInclusiveFiltersAndOrderByDateThenId() {
		var date = LocalDate.of(2026, 9, 28);
		var otherCustomer = customers.save(account, Customer.create("Other")).getId();
		orders.save(account, work(date.minusDays(1), customer, "1", "10"));
		var a = orders.save(account, work(date, customer, "1", "10"));
		var b = orders.save(account, work(date, customer, "1", "10"));
		var c = orders.save(account, work(date.plusDays(1), customer, "1", "10"));
		orders.save(account, work(date.plusDays(2), customer, "1", "10"));
		orders.save(account, work(date, otherCustomer, "1", "10"));
		var list = orders.findAll(account, date, date.plusDays(1), customer);
		assertEquals(3, list.size());
		assertEquals(c.id(), list.getLast().id());
		assertEquals(List.of(a.id(), b.id()).stream().sorted(Comparator.comparing(UUID::toString)).toList(),
				list.subList(0, 2).stream().map(WorkOrder::id).toList());
		assertTrue(orders.findAll(foreignAccount, null, null, null).isEmpty());
		assertTrue(orders.findById(foreignAccount, a.id()).isEmpty());
	}

	String body(UUID customerId, UUID locationId, List<UUID> participants, String extras) {
		return "{\"customerId\":\"" + customerId + "\",\"customerLocationId\":"
				+ (locationId == null ? "null" : "\"" + locationId + "\"")
				+ ",\"serviceDate\":\"2026-09-28\",\"contractedHours\":3,\"hourlyRate\":11.50,\"participantIds\":"
				+ participants.stream()
					.map(id -> "\"" + id + "\"")
					.collect(java.util.stream.Collectors.joining(",", "[", "]"))
				+ extras + "}";
	}

	@Test
	void shouldReturnFullRepresentationAndUseServerControlledValues() throws Exception {
		accounts.update(accounts.findById(account).orElseThrow().withPreferences("UTC", DefaultWorkStatus.COMPLETED));
		var result = mvc.perform(post("/api/v1/work-orders").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(customer, location, List.of(second, person),
					",\"startTime\":\"10:30:00\",\"description\":\"Visit\",\"totalAmount\":1,\"currencyCode\":\"USD\",\"organizationId\":\""
							+ foreignAccount + "\"")))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.customer").value("Original"))
			.andExpect(jsonPath("$.customerLocationId").value(location.toString()))
			.andExpect(jsonPath("$.totalAmount").value(34.50))
			.andExpect(jsonPath("$.currencyCode").value("GBP"))
			.andExpect(jsonPath("$.status").value("COMPLETED"))
			.andExpect(jsonPath("$.version").value(0))
			.andExpect(jsonPath("$.participantCount").value(2))
			.andExpect(jsonPath("$.allocationPolicyVersion").value(1))
			.andExpect(jsonPath("$.assignments[0].collaboratorId").value(second.toString()))
			.andExpect(jsonPath("$.assignments[0].inclusionPosition").value(0))
			.andExpect(jsonPath("$.assignments[0].allocatedAmount").value(17.25))
			.andReturn();
		String path = result.getResponse().getHeader("Location");
		mvc.perform(get(path).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(content().json(result.getResponse().getContentAsString()));
		mvc.perform(get("/api/v1/work-orders").with(user(principal))
			.param("from", "2026-09-28")
			.param("to", "2026-09-28")
			.param("customerId", customer.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
		mvc.perform(
				get("/api/v1/work-orders").with(user(principal)).param("from", "2026-09-29").param("to", "2026-09-28"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectInvalidReferencesAndLegacyContractsAndPreserveSecurity() throws Exception {
		var foreignCustomer = customers.save(foreignAccount, Customer.create("Foreign")).getId();
		var foreignPerson = collaborators.save(foreignAccount, Collaborator.create(foreignAccount, "Foreign", null))
			.getId();
		var other = customers.save(account, Customer.create("Other")).getId();
		for (String invalid : List.of(body(foreignCustomer, null, List.of(person), ""),
				body(customer, null, List.of(foreignPerson), ""), body(other, location, List.of(person), ""),
				body(customer, UUID.randomUUID(), List.of(person), ""),
				body(customer, null, List.of(UUID.randomUUID()), ""))) {
			mvc.perform(post("/api/v1/work-orders").with(user(principal))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalid))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"));
		}
		for (String invalid : List.of(body(customer, null, List.of(), ""),
				body(customer, null, List.of(person, person), ""),
				body(customer, null, List.of(person), ",\"initialStatus\":\"CANCELLED\""),
				"{\"customerId\":\"" + customer
						+ "\",\"contractedHours\":4,\"hourlyRate\":10,\"employeeCount\":2,\"serviceDate\":\"2026-09-28\"}")) {
			mvc.perform(post("/api/v1/work-orders").with(user(principal))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalid)).andExpect(status().isBadRequest());
		}
		collaborators.save(account, collaborators.findById(account, person).orElseThrow().deactivate());
		mvc.perform(post("/api/v1/work-orders").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(customer, null, List.of(person), ""))).andExpect(status().isConflict());
		mvc.perform(get("/api/v1/work-orders")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/work-orders").with(user(principal))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(customer, null, List.of(second), ""))).andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/order-services").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).andExpect(status().isForbidden());
		var saved = orders.save(account, WorkOrder.create(customer, null, LocalDate.now(), null, null, BigDecimal.ONE,
				BigDecimal.TEN, List.of(second), WorkOrderStatus.COMPLETED));
		mvc.perform(delete("/api/v1/work-orders/" + saved.id()).with(user(principal)).with(csrf()))
			.andExpect(status().isMethodNotAllowed());
		assertTrue(orders.findById(account, saved.id()).isPresent());
	}

	@Autowired
	CompleteWorkOrder complete;

	@Autowired
	RescheduleWorkOrder reschedule;

	@Autowired
	com.klaus.moply.workflows.application.CancelWorkOrder cancel;

	@Autowired
	com.klaus.moply.workorders.application.ports.WorkOrderOperations operations;

	@Autowired
	org.springframework.transaction.PlatformTransactionManager transactionManager;

	@org.springframework.boot.test.context.TestConfiguration
	static class FixedTime {

		@org.springframework.context.annotation.Bean
		@org.springframework.context.annotation.Primary
		Clock testClock() {
			return Clock.fixed(Instant.parse("2026-09-30T23:30:00Z"), ZoneOffset.UTC);
		}

	}

	@Test
	void shouldPreserveIdentityConditionsAndAssignmentRowsThroughLifecycle() {
		var context = new Context(account);
		var saved = orders.save(account,
				WorkOrder.create(customer, location, LocalDate.of(2026, 10, 5), LocalTime.NOON, "Historic",
						new BigDecimal("3"), new BigDecimal("11.50"), List.of(second, person),
						WorkOrderStatus.SCHEDULED));
		var rows = assignmentRows(saved.id());
		collaborators.save(account, collaborators.findById(account, person).orElseThrow().deactivate());
		complete.execute(context, saved.id());
		complete.execute(context, saved.id());
		assertEquals(1, orders.findById(account, saved.id()).orElseThrow().version());
		reschedule.execute(context, new RescheduleWorkOrder.Input(saved.id(), LocalDate.of(2026, 10, 6), null));
		var moved = orders.findById(account, saved.id()).orElseThrow();
		assertEquals(WorkOrderStatus.COMPLETED, moved.status());
		assertEquals(2, moved.version());
		assertNull(moved.startTime());
		cancel.execute(context, saved.id());
		var cancelled = orders.findById(account, saved.id()).orElseThrow();
		cancel.execute(context, saved.id());
		assertEquals(cancelled, orders.findById(account, saved.id()).orElseThrow());
		assertEquals(3, cancelled.version());
		assertEquals(WorkOrderStatus.CANCELLED, cancelled.status());
		assertEquals(rows, assignmentRows(saved.id()));
		assertEquals(saved.assignments(), cancelled.assignments());
		assertEquals(saved.totalAmount(), cancelled.totalAmount());
		assertEquals(saved.customerLocationId(), cancelled.customerLocationId());
		assertEquals(saved.description(), cancelled.description());
		assertEquals(saved.hourlyRate(), cancelled.hourlyRate());
		assertEquals(saved.contractedHours(), cancelled.contractedHours());
		assertEquals(saved.currencyCode(), cancelled.currencyCode());
		assertEquals(saved.allocationPolicyVersion(), cancelled.allocationPolicyVersion());
		assertThrows(com.klaus.moply.workorders.domain.exception.WorkOrderStateException.class,
				() -> complete.execute(context, saved.id()));
		assertThrows(com.klaus.moply.workorders.domain.exception.WorkOrderStateException.class, () -> reschedule
			.execute(context, new RescheduleWorkOrder.Input(saved.id(), LocalDate.of(2026, 10, 7), null)));
		assertEquals(1,
				orders.findAll(account, moved.serviceDate(), moved.serviceDate(), customer, WorkOrderStatus.CANCELLED)
					.size());
		assertTrue(
				orders.findAll(account, moved.serviceDate(), moved.serviceDate(), customer, WorkOrderStatus.COMPLETED)
					.isEmpty());
	}

	List<Map<String, Object>> assignmentRows(UUID id) {
		return jdbc.queryForList("SELECT * FROM tb_work_assignment WHERE work_order_id=? ORDER BY inclusion_position",
				id);
	}

	@Test
	void shouldRollbackCancellationWithOuterWorkflowFailure() {
		var saved = orders.save(account, work(LocalDate.of(2026, 10, 5), customer, "3", "11.50"));
		var rows = assignmentRows(saved.id());
		assertThrows(IllegalStateException.class,
				() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager)
					.executeWithoutResult(status -> {
						cancel.execute(new Context(account), saved.id());
						throw new IllegalStateException("Failure after cancellation");
					}));
		assertEquals(saved, orders.findById(account, saved.id()).orElseThrow());
		assertEquals(rows, assignmentRows(saved.id()));
	}

	@Test
	void shouldSerializeRepeatedCancellationAndConcurrentReschedule() throws Exception {
		for (boolean rescheduling : List.of(false, true)) {
			var saved = orders.save(account, work(LocalDate.of(2026, 10, 5), customer, "3", "11.50"));
			var locked = new java.util.concurrent.CountDownLatch(1);
			var release = new java.util.concurrent.CountDownLatch(1);
			var attempted = new java.util.concurrent.CountDownLatch(1);
			try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
				var first = executor
					.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager)
						.executeWithoutResult(status -> {
							cancel.execute(new Context(account), saved.id());
							locked.countDown();
							await(release);
						}));
				assertTrue(locked.await(10, java.util.concurrent.TimeUnit.SECONDS));
				var secondCall = executor.submit(() -> {
					attempted.countDown();
					if (rescheduling) {
						assertThrows(com.klaus.moply.workorders.domain.exception.WorkOrderStateException.class,
								() -> reschedule.execute(new Context(account),
										new RescheduleWorkOrder.Input(saved.id(), LocalDate.of(2026, 10, 6), null)));
					}
					else
						cancel.execute(new Context(account), saved.id());
				});
				try {
					assertTrue(attempted.await(10, java.util.concurrent.TimeUnit.SECONDS));
				}
				finally {
					release.countDown();
				}
				first.get(15, java.util.concurrent.TimeUnit.SECONDS);
				secondCall.get(15, java.util.concurrent.TimeUnit.SECONDS);
			}
			finally {
				release.countDown();
			}
			var result = orders.findById(account, saved.id()).orElseThrow();
			assertEquals(WorkOrderStatus.CANCELLED, result.status());
			assertEquals(1, result.version());
			assertEquals(saved.serviceDate(), result.serviceDate());
			assertEquals(saved.assignments(), result.assignments());
		}
	}

	static void await(java.util.concurrent.CountDownLatch latch) {
		try {
			if (!latch.await(10, java.util.concurrent.TimeUnit.SECONDS))
				throw new IllegalStateException("Timed out waiting for concurrent operation");
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	@Test
	void shouldExposeCommandsWithSecurityIsolationAndStateConflicts() throws Exception {
		var saved = orders.save(account, work(LocalDate.of(2026, 10, 5), customer, "3", "11.50"));
		var foreignPrincipal = new AccountPrincipal(
				new AppUser(UUID.randomUUID(), foreignAccount, new LoginEmail("foreign@b"), "unused"));
		for (String command : List.of("complete", "reschedule", "cancel")) {
			var path = "/api/v1/work-orders/" + saved.id() + "/" + command;
			var body = "{\"serviceDate\":\"2026-10-06\"}";
			mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
			mvc.perform(post(path).with(user(principal)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
			mvc.perform(post(path).with(user(foreignPrincipal))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)).andExpect(status().isNotFound());
			mvc.perform(post("/api/v1/work-orders/" + UUID.randomUUID() + "/" + command).with(user(principal))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)).andExpect(status().isNotFound());
		}
		assertEquals(saved, orders.findById(account, saved.id()).orElseThrow());
		String base = "/api/v1/work-orders/" + saved.id();
		mvc.perform(post(base + "/complete").with(user(principal)).with(csrf())).andExpect(status().isNoContent());
		mvc.perform(post(base + "/reschedule").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).andExpect(status().isBadRequest());
		mvc.perform(post(base + "/reschedule").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"serviceDate\":\"2026-10-01\",\"startTime\":\"09:30:00\"}")).andExpect(status().isNoContent());
		accounts.update(
				accounts.findById(account).orElseThrow().withPreferences("Europe/London", DefaultWorkStatus.SCHEDULED));
		// At 23:30 UTC it is already October 1st in London: completed work cannot move
		// again.
		mvc.perform(post(base + "/reschedule").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"serviceDate\":\"2026-10-02\"}")).andExpect(status().isConflict());
		for (int i = 0; i < 2; i++)
			mvc.perform(post(base + "/cancel").with(user(principal)).with(csrf())).andExpect(status().isNoContent());
		mvc.perform(post(base + "/complete").with(user(principal)).with(csrf())).andExpect(status().isConflict());
		mvc.perform(get(base).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
		mvc.perform(get(base).with(user(foreignPrincipal))).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/work-orders").with(user(principal))
			.param("from", "2026-10-01")
			.param("to", "2026-10-01")
			.param("customerId", customer.toString())
			.param("status", "CANCELLED")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mvc.perform(get("/api/v1/work-orders").with(user(principal)).param("status", "SCHEDULED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/v1/work-orders").with(user(foreignPrincipal)).param("status", "CANCELLED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Autowired
	CreateWorkOrder create;

	@Test
	void shouldRescheduleFutureCreatedFromPreferenceAndKeepExistingStatesAfterPreferenceChange() {
		var context = new Context(account);
		accounts.update(accounts.findById(account).orElseThrow().withPreferences("UTC", DefaultWorkStatus.COMPLETED));
		var date = LocalDate.of(2026, 10, 10);
		var input = new com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput(customer, location,
				date, null, "Future", BigDecimal.ONE, BigDecimal.TEN, List.of(person, second), null);
		var future = create.execute(context, input);
		assertEquals(WorkOrderStatus.COMPLETED, future.status());
		var override = create.execute(context,
				new com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput(customer, null, date, null,
						null, BigDecimal.ONE, BigDecimal.TEN, List.of(person), WorkOrderStatus.SCHEDULED));
		assertEquals(WorkOrderStatus.SCHEDULED, override.status());
		accounts.update(accounts.findById(account).orElseThrow().withPreferences("UTC", DefaultWorkStatus.SCHEDULED));
		assertEquals(WorkOrderStatus.COMPLETED, find.execute(context, future.id()).status());
		reschedule.execute(context, new RescheduleWorkOrder.Input(future.id(), date.plusDays(1), LocalTime.NOON));
		var updated = find.execute(context, future.id());
		assertEquals(WorkOrderStatus.COMPLETED, updated.status());
		assertEquals(future.assignments(), updated.assignments());
		assertEquals(date.plusDays(1), updated.serviceDate());
		assertEquals(WorkOrderStatus.SCHEDULED, create.execute(context, input).status());
		var explicitCompleted = create.execute(context,
				new com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput(customer, null, date, null,
						null, BigDecimal.ONE, BigDecimal.TEN, List.of(person), WorkOrderStatus.COMPLETED));
		assertEquals(WorkOrderStatus.COMPLETED, explicitCompleted.status());
	}

}
