package com.klaus.moply.workorders.infra.persistence;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.factory.PostgresSpringIntegrationTest;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.workflows.application.usecase.RescheduleWorkOrder;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleWorkOrderInput;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.CompleteWorkOrder;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.workorders.application.usecase.FindWorkOrderById;
import com.klaus.moply.workorders.domain.entity.WorkAssignment;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;
import com.klaus.moply.workorders.domain.vo.WorkOrderDescription;
import com.klaus.moply.workorders.domain.vo.WorkOrderSchedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "spring.jpa.open-in-view=false", "spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkOrderIntegrationTest extends PostgresSpringIntegrationTest {

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
		jdbc.update("DELETE FROM tb_collaborator_payment");
		jdbc.update("DELETE FROM tb_customer_payment");
		jdbc.update("DELETE FROM tb_work_assignment");
		jdbc.update("DELETE FROM tb_order_service");
		jdbc.update("DELETE FROM tb_customer_location");
		jdbc.update("DELETE FROM tb_customer");
		jdbc.update("DELETE FROM tb_collaborator");
		jdbc.update("DELETE FROM tb_app_user");
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
		return WorkOrder.create(c, null, new WorkOrderSchedule(date, null), new WorkOrderDescription(null),
				new DurationHours(new BigDecimal(hours)), new HourlyRate(new BigDecimal(rate)), List.of(second, person),
				WorkOrderStatus.SCHEDULED);
	}

	@Test
	void shouldRoundTripZeroAndLargeValuesWithoutRedistributionAndRetainHistory() {
		for (String hours : List.of("0.01", "123456789012345678901234567890.12")) {
			var saved = orders.save(account, work(LocalDate.now(), customer, hours, "1.00"));
			var loaded = orders.findById(account, saved.id()).orElseThrow();
			assertSameState(saved, loaded);
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
		assertSameState(saved, orders.findById(account, saved.id()).orElseThrow());
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
		var list = orders.findAll(account, new WorkOrderDateRange(date, date.plusDays(1)), customer);
		assertEquals(3, list.size());
		assertEquals(c.id(), list.getLast().id());
		assertEquals(List.of(a.id(), b.id()).stream().sorted(Comparator.comparing(UUID::toString)).toList(),
				list.subList(0, 2).stream().map(WorkOrder::id).toList());
		assertTrue(orders.findAll(foreignAccount, new WorkOrderDateRange(null, null), null).isEmpty());
		assertTrue(orders.findById(foreignAccount, a.id()).isEmpty());
	}

	String body(UUID customerId, UUID locationId, List<UUID> participants, String extras) {
		return "{\"serviceDate\":\"2026-09-28\",\"conditions\":{\"customerId\":\"" + customerId + "\",\"customerLocationId\":"
				+ (locationId == null ? "null" : "\"" + locationId + "\"")
				+ ",\"contractedHours\":3,\"hourlyRate\":11.50,\"participantIds\":"
				+ participants.stream()
					.map(id -> "\"" + id + "\"")
					.collect(java.util.stream.Collectors.joining(",", "[", "]"))
				+ extras + "}}";
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
			.andExpect(jsonPath("$.customer.name").value("Original"))
			.andExpect(jsonPath("$.customerLocationId").value(location.toString()))
			.andExpect(jsonPath("$.pricing.totalAmount").value(34.50))
			.andExpect(jsonPath("$.pricing.currencyCode").value("GBP"))
			.andExpect(jsonPath("$.status").value("COMPLETED"))
			.andExpect(jsonPath("$.version").value(0))
			.andExpect(jsonPath("$.participantCount").value(2))
			.andExpect(jsonPath("$.pricing.allocationPolicyVersion").value(1))
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
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(
				get("/api/v1/work-orders").with(user(principal)).param("from", "2026-09-30").param("to", "2026-09-28"))
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
		var saved = orders.save(account,
				WorkOrder.create(customer, null, new WorkOrderSchedule(LocalDate.now(), null),
						new WorkOrderDescription(null), new DurationHours(BigDecimal.ONE),
						new HourlyRate(BigDecimal.TEN), List.of(second), WorkOrderStatus.COMPLETED));
		mvc.perform(delete("/api/v1/work-orders/" + saved.id()).with(user(principal)).with(csrf()))
			.andExpect(status().isMethodNotAllowed());
		assertTrue(orders.findById(account, saved.id()).isPresent());
	}

	@Autowired
	CompleteWorkOrder complete;

	@Autowired
	RescheduleWorkOrder reschedule;

	@Autowired
	com.klaus.moply.workflows.application.usecase.CancelWorkOrder cancel;

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
				WorkOrder.create(customer, location, new WorkOrderSchedule(LocalDate.of(2026, 10, 5), LocalTime.NOON),
						new WorkOrderDescription("Historic"), new DurationHours(new BigDecimal("3")),
						new HourlyRate(new BigDecimal("11.50")), List.of(second, person), WorkOrderStatus.SCHEDULED));
		var rows = assignmentRows(saved.id());
		collaborators.save(account, collaborators.findById(account, person).orElseThrow().deactivate());
		complete.execute(context, saved.id());
		complete.execute(context, saved.id());
		assertEquals(1, orders.findById(account, saved.id()).orElseThrow().version());
		reschedule.execute(context, new RescheduleWorkOrderInput(saved.id(), LocalDate.of(2026, 10, 6), null));
		var moved = orders.findById(account, saved.id()).orElseThrow();
		assertEquals(WorkOrderStatus.COMPLETED, moved.status());
		assertEquals(2, moved.version());
		assertNull(moved.startTime());
		cancel.execute(context, new CancelWorkOrderInput(saved.id(), null, false, null));
		var cancelled = orders.findById(account, saved.id()).orElseThrow();
		cancel.execute(context, new CancelWorkOrderInput(saved.id(), null, false, null));
		assertSameState(cancelled, orders.findById(account, saved.id()).orElseThrow());
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
			.execute(context, new RescheduleWorkOrderInput(saved.id(), LocalDate.of(2026, 10, 7), null)));
		assertEquals(1,
				orders
					.findAll(account, new WorkOrderDateRange(moved.serviceDate(), moved.serviceDate()), customer,
							WorkOrderStatus.CANCELLED)
					.size());
		assertTrue(orders
			.findAll(account, new WorkOrderDateRange(moved.serviceDate(), moved.serviceDate()), customer,
					WorkOrderStatus.COMPLETED)
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
						cancel.execute(new Context(account), new CancelWorkOrderInput(saved.id(), null, false, null));
						throw new IllegalStateException("Failure after cancellation");
					}));
		assertSameState(saved, orders.findById(account, saved.id()).orElseThrow());
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
							cancel.execute(new Context(account),
									new CancelWorkOrderInput(saved.id(), null, false, null));
							locked.countDown();
							await(release);
						}));
				assertTrue(locked.await(10, java.util.concurrent.TimeUnit.SECONDS));
				var secondCall = executor.submit(() -> {
					attempted.countDown();
					if (rescheduling) {
						assertThrows(com.klaus.moply.workorders.domain.exception.WorkOrderStateException.class,
								() -> reschedule.execute(new Context(account),
										new RescheduleWorkOrderInput(saved.id(), LocalDate.of(2026, 10, 6), null)));
					}
					else
						cancel.execute(new Context(account), new CancelWorkOrderInput(saved.id(), null, false, null));
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
		assertSameState(saved, orders.findById(account, saved.id()).orElseThrow());
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
			.param("status", "CANCELLED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/work-orders").with(user(principal))
			.param("status", "CANCELLED")
			.param("page", "1")
			.param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/work-orders").with(user(principal)).param("status", "SCHEDULED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/v1/work-orders").with(user(foreignPrincipal)).param("status", "CANCELLED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0));
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
		reschedule.execute(context, new RescheduleWorkOrderInput(future.id(), date.plusDays(1), LocalTime.NOON));
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

	@Autowired
	com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository payments;

	@Autowired
	com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment recordPayment;

	@Autowired
	com.klaus.moply.payments.application.usecase.RecordCollaboratorPayment recordCollaboratorPayment;

	@Autowired
	com.klaus.moply.workflows.application.usecase.RescheduleWorkOrder workflowReschedule;

	@Test
	void shouldRecordIntegralPaymentWithinAccountDateAndProtectPaymentRoutes() throws Exception {
		accounts.update(
				accounts.findById(account).orElseThrow().withPreferences("Europe/London", DefaultWorkStatus.SCHEDULED));
		var todayInLondon = LocalDate.of(2026, 10, 1);
		var saved = orders.save(account, work(todayInLondon, customer, "3", "11.50"));
		var path = "/api/v1/work-orders/" + saved.id() + "/payments";
		mvc.perform(post(path).with(user(principal))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"paidOn\":\"2026-10-01\"}")).andExpect(status().isForbidden());
		mvc.perform(post(path).with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"paidOn\":\"2026-09-30\"}")).andExpect(status().isBadRequest());
		mvc.perform(post(path).with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"paidOn\":\"2026-10-02\"}")).andExpect(status().isBadRequest());
		var result = mvc
			.perform(post(path).with(user(principal))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"paidOn\":\"2026-10-01\",\"amount\":0.01,\"currencyCode\":\"USD\",\"organizationId\":\""
						+ foreignAccount + "\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.amount").value(34.50))
			.andExpect(jsonPath("$.currencyCode").value("GBP"))
			.andExpect(jsonPath("$.status").value("RECORDED"))
			.andExpect(jsonPath("$.recordedBy").value(principal.getUserId().toString()))
			.andReturn();
		var paymentId = UUID
			.fromString(com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
		mvc.perform(post(path).with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"paidOn\":\"2026-10-01\"}")).andExpect(status().isConflict());
		mvc.perform(get(path).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(paymentId.toString()))
			.andExpect(jsonPath("$.totalElements").value(1));
		assertTrue(payments.findById(foreignAccount, paymentId).isEmpty());
		var foreignPrincipal = new AccountPrincipal(
				new AppUser(UUID.randomUUID(), foreignAccount, new LoginEmail("foreign-payment@b"), "unused"));
		mvc.perform(get(path).with(user(foreignPrincipal))).andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/payments/" + paymentId + "/reversal").with(user(principal))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"confirmNoMoneyReceived\":true,\"reason\":\"wrong\"}")).andExpect(status().isForbidden());
		var future = orders.save(account, work(todayInLondon.plusDays(1), customer, "3", "11.50"));
		mvc.perform(post("/api/v1/work-orders/" + future.id() + "/payments").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"paidOn\":\"2026-10-02\"}")).andExpect(status().isConflict());
		var completed = orders.save(account, work(todayInLondon.minusDays(1), customer, "3", "11.50"));
		complete.execute(new Context(account), completed.id());
		var completedPayment = recordPayment.execute(new Context(account),
				new com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment.Input(completed.id(),
						todayInLondon, principal.getUserId()));
		assertEquals(com.klaus.moply.payments.domain.Payment.Status.RECORDED, completedPayment.status());
		var cancelled = orders.save(account, work(todayInLondon.minusDays(1), customer, "3", "11.50"));
		cancel.execute(new Context(account), new CancelWorkOrderInput(cancelled.id(), null, false, null));
		assertThrows(com.klaus.moply.payments.application.usecase.exception.PaymentConflictException.class,
				() -> recordPayment.execute(new Context(account),
						new com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment.Input(cancelled.id(),
								todayInLondon, principal.getUserId())));
	}

	@Test
	void shouldReverseWithAuditAndCancelAtomicallyOnlyAfterConfirmation() throws Exception {
		var saved = orders.save(account, work(LocalDate.of(2026, 9, 30), customer, "3", "11.50"));
		var payment = recordPayment.execute(new Context(account),
				new com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment.Input(saved.id(),
						LocalDate.of(2026, 9, 30), principal.getUserId()));
		var cancelPath = "/api/v1/work-orders/" + saved.id() + "/cancel";
		mvc.perform(post(cancelPath).with(user(principal)).with(csrf())).andExpect(status().isConflict());
		assertEquals(com.klaus.moply.payments.domain.Payment.Status.RECORDED,
				payments.findById(account, payment.id()).orElseThrow().status());
		var reversePath = "/api/v1/payments/" + payment.id() + "/reversal";
		mvc.perform(post(reversePath).with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"confirmNoMoneyReceived\":false,\"reason\":\"mistake\"}")).andExpect(status().isBadRequest());
		mvc.perform(post(reversePath).with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"confirmNoMoneyReceived\":true,\"reason\":\"  \"}")).andExpect(status().isBadRequest());
		mvc.perform(post(reversePath).with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"confirmNoMoneyReceived\":true,\"reason\":\"Lançamento incorreto\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REVERSED"))
			.andExpect(jsonPath("$.reversedBy").value(principal.getUserId().toString()));
		var reversed = payments.findById(account, payment.id()).orElseThrow();
		assertEquals(payment.amount(), reversed.amount());
		assertEquals(payment.recordedAt(), reversed.recordedAt());
		assertEquals("Lançamento incorreto", reversed.reversal().reason());
		var replacement = recordPayment.execute(new Context(account),
				new com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment.Input(saved.id(),
						LocalDate.of(2026, 9, 30), principal.getUserId()));
		assertNotEquals(payment.id(), replacement.id());
		assertEquals(2, payments.findAllByWork(account, saved.id()).size());
		assertThrows(IllegalStateException.class,
				() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager)
					.executeWithoutResult(status -> {
						cancel.execute(new Context(account),
								new com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput(saved.id(),
										principal.getUserId(), true, "Erro"));
						throw new IllegalStateException("rollback");
					}));
		assertEquals(WorkOrderStatus.SCHEDULED, orders.findById(account, saved.id()).orElseThrow().status());
		assertEquals(com.klaus.moply.payments.domain.Payment.Status.RECORDED,
				payments.findById(account, replacement.id()).orElseThrow().status());
		mvc.perform(post(cancelPath).with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"confirmNoMoneyReceived\":true,\"reason\":\"Lançamento incorreto\"}"))
			.andExpect(status().isNoContent());
		assertEquals(WorkOrderStatus.CANCELLED, orders.findById(account, saved.id()).orElseThrow().status());
		assertEquals(com.klaus.moply.payments.domain.Payment.Status.REVERSED,
				payments.findById(account, replacement.id()).orElseThrow().status());
	}

	@Test
	void shouldSerializePaymentAgainstRescheduleOnPostgres() throws Exception {
		var saved = orders.save(account, work(LocalDate.of(2026, 9, 30), customer, "3", "11.50"));
		var start = new java.util.concurrent.CountDownLatch(1);
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			var payment = executor.submit(() -> {
				await(start);
				try {
					recordPayment.execute(new Context(account),
							new com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment.Input(saved.id(),
									LocalDate.of(2026, 9, 30), principal.getUserId()));
					return "paid";
				}
				catch (RuntimeException e) {
					return "payment rejected";
				}
			});
			var reschedule = executor.submit(() -> {
				await(start);
				try {
					workflowReschedule.execute(new Context(account),
							new com.klaus.moply.workflows.application.usecase.dto.RescheduleWorkOrderInput(saved.id(),
									LocalDate.of(2026, 10, 1), null));
					return "moved";
				}
				catch (RuntimeException e) {
					return "reschedule rejected";
				}
			});
			start.countDown();
			var outcome = List.of(payment.get(15, java.util.concurrent.TimeUnit.SECONDS),
					reschedule.get(15, java.util.concurrent.TimeUnit.SECONDS));
			var persisted = orders.findById(account, saved.id()).orElseThrow();
			var active = payments.findActiveByWork(account, saved.id());
			assertFalse(outcome.contains("paid") && outcome.contains("moved"));
			assertFalse(persisted.serviceDate().isAfter(LocalDate.of(2026, 9, 30)) && active.isPresent());
		}
	}

	@Test
	void shouldSerializeConcurrentDuplicatePaymentAndPaymentAgainstCancellationOnPostgres() throws Exception {
		var saved = orders.save(account, work(LocalDate.of(2026, 9, 30), customer, "3", "11.50"));
		var start = new java.util.concurrent.CountDownLatch(1);
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> {
				await(start);
				try {
					recordPayment.execute(new Context(account),
							new com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment.Input(saved.id(),
									LocalDate.of(2026, 9, 30), principal.getUserId()));
					return true;
				}
				catch (RuntimeException e) {
					return false;
				}
			});
			var second = executor.submit(() -> {
				await(start);
				try {
					recordPayment.execute(new Context(account),
							new com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment.Input(saved.id(),
									LocalDate.of(2026, 9, 30), principal.getUserId()));
					return true;
				}
				catch (RuntimeException e) {
					return false;
				}
			});
			start.countDown();
			assertEquals(1, (first.get(15, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0)
					+ (second.get(15, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0));
		}
		assertEquals(1,
				payments.findAllByWork(account, saved.id())
					.stream()
					.filter(p -> p.status() == com.klaus.moply.payments.domain.Payment.Status.RECORDED)
					.count());
		var another = orders.save(account, work(LocalDate.of(2026, 9, 30), customer, "3", "11.50"));
		var race = new java.util.concurrent.CountDownLatch(1);
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			var pay = executor.submit(() -> {
				await(race);
				try {
					recordPayment.execute(new Context(account),
							new com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment.Input(another.id(),
									LocalDate.of(2026, 9, 30), principal.getUserId()));
				}
				catch (RuntimeException ignored) {
				}
			});
			var cancelWork = executor.submit(() -> {
				await(race);
				cancel.execute(new Context(account),
						new com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput(another.id(),
								principal.getUserId(), true, "Cancelamento simultâneo"));
			});
			race.countDown();
			pay.get(15, java.util.concurrent.TimeUnit.SECONDS);
			cancelWork.get(15, java.util.concurrent.TimeUnit.SECONDS);
		}
		assertEquals(WorkOrderStatus.CANCELLED, orders.findById(account, another.id()).orElseThrow().status());
		assertTrue(payments.findActiveByWork(account, another.id()).isEmpty());
	}

	@Test
	void shouldRecordCollaboratorAcertoWithIdempotencyReversalAndCancellationGuard() throws Exception {
		var saved = orders.save(account, work(LocalDate.of(2026, 9, 30), customer, "3", "11.50"));
		var base = "/api/v1/work-orders/" + saved.id() + "/collaborators/" + person + "/payments";
		var foreignPrincipal = new AccountPrincipal(
				new AppUser(UUID.randomUUID(), foreignAccount, new LoginEmail("foreign@b"), "unused"));
		mvc.perform(get(base).with(user(foreignPrincipal))).andExpect(status().isNotFound());
		var content = "{\"amount\":10.00,\"paidOn\":\"2026-09-30\"}";
		var first = mvc
			.perform(post(base).with(user(principal))
				.with(csrf())
				.header("Idempotency-Key", "collab-payment-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.amount").value(10.00))
			.andReturn();
		var paymentId = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder()
			.build()
			.readTree(first.getResponse().getContentAsString())
			.get("id")
			.asText());
		mvc.perform(post(base).with(user(principal))
			.with(csrf())
			.header("Idempotency-Key", "collab-payment-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(content)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(paymentId.toString()));
		mvc.perform(post(base).with(user(principal))
			.with(csrf())
			.header("Idempotency-Key", "collab-payment-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":5.00,\"paidOn\":\"2026-09-30\"}")).andExpect(status().isConflict());
		mvc.perform(post(base).with(user(principal))
			.with(csrf())
			.header("Idempotency-Key", "collab-payment-2")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":7.26,\"paidOn\":\"2026-09-30\"}")).andExpect(status().isConflict());
		mvc.perform(post(base + "/" + paymentId + "/reversal").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"confirmNotActuallyPaid\":false,\"reason\":\"Recorded in error\"}"))
			.andExpect(status().isBadRequest());
		collaborators.save(account, collaborators.findById(account, person).orElseThrow().deactivate());
		mvc.perform(post(base + "/" + paymentId + "/reversal").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"confirmNotActuallyPaid\":true,\"reason\":\"Recorded in error\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REVERSED"))
			.andExpect(jsonPath("$.reversalReason").value("Recorded in error"));
		mvc.perform(post(base).with(user(principal))
			.with(csrf())
			.header("Idempotency-Key", "collab-payment-3")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":7.25,\"paidOn\":\"2026-09-30\"}")).andExpect(status().isCreated());
		mvc.perform(post(base).with(user(principal))
			.with(csrf())
			.header("Idempotency-Key", "collab-payment-4")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":10.00,\"paidOn\":\"2026-09-30\"}")).andExpect(status().isCreated());
		mvc.perform(post("/api/v1/work-orders/" + saved.id() + "/complete").with(user(principal)).with(csrf()))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/collaborators/" + person + "/payments/summary").with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.allocatedAmount").value(17.25))
			.andExpect(jsonPath("$.recordedAmount").value(17.25))
			.andExpect(jsonPath("$.remainingAmount").value(0.00))
			.andExpect(jsonPath("$.requiresAttention").value(false));
		mvc.perform(post("/api/v1/work-orders/" + saved.id() + "/cancel").with(user(principal)).with(csrf()))
			.andExpect(status().isConflict());
		mvc.perform(get(base).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(3))
			.andExpect(jsonPath("$.totalElements").value(3));
		mvc.perform(get(base).with(user(principal)).param("page", "1").param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.page").value(1));
		mvc.perform(post(base).with(user(principal))
			.header("Idempotency-Key", "csrf-required")
			.contentType(MediaType.APPLICATION_JSON)
			.content(content)).andExpect(status().isForbidden());
	}

	@Test
	void shouldIncludeOnlyActiveWorksAndSignalCompletedOutstandingWorkAfterItsDate() throws Exception {
		accounts.update(
				accounts.findById(account).orElseThrow().withPreferences("Europe/London", DefaultWorkStatus.SCHEDULED));
		var today = LocalDate.of(2026, 10, 1);
		var due = orders.save(account,
				WorkOrder.create(customer, null, new WorkOrderSchedule(today, null), new WorkOrderDescription(null),
						new DurationHours(new BigDecimal("3")), new HourlyRate(new BigDecimal("11.50")),
						List.of(person), WorkOrderStatus.COMPLETED));
		var future = orders.save(account,
				WorkOrder.create(customer, null, new WorkOrderSchedule(today.plusDays(1), null),
						new WorkOrderDescription(null), new DurationHours(new BigDecimal("3")),
						new HourlyRate(new BigDecimal("11.50")), List.of(person), WorkOrderStatus.COMPLETED));
		var foreignPrincipal = new AccountPrincipal(
				new AppUser(UUID.randomUUID(), foreignAccount, new LoginEmail("foreign@b"), "unused"));
		mvc.perform(get("/api/v1/collaborators/" + person + "/payments/summary").with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.currencyCode").value("GBP"))
			.andExpect(jsonPath("$.remainingAmount").value(69.00))
			.andExpect(jsonPath("$.workOrders[0].workOrderId").value(due.id().toString()))
			.andExpect(jsonPath("$.workOrders[0].requiresAttention").value(true))
			.andExpect(jsonPath("$.workOrders[1].workOrderId").value(future.id().toString()))
			.andExpect(jsonPath("$.workOrders[1].requiresAttention").value(false));
		mvc.perform(get("/api/v1/collaborators/" + person + "/payments/summary").with(user(foreignPrincipal)))
			.andExpect(status().isNotFound());
	}

	@Test
	void shouldSerializeConcurrentAcertosAgainstTheSameAllocationAndLifecycleOperations() throws Exception {
		var context = new Context(account);
		var saved = orders.save(account, work(LocalDate.of(2026, 9, 30), customer, "3", "11.50"));
		var gate = new java.util.concurrent.CountDownLatch(1);
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> {
				await(gate);
				try {
					recordCollaboratorPayment.execute(context,
							new com.klaus.moply.payments.application.usecase.RecordCollaboratorPayment.Input(saved.id(),
									person, new BigDecimal("10.00"), LocalDate.of(2026, 9, 30), "concurrent-1",
									principal.getUserId()));
					return true;
				}
				catch (com.klaus.moply.payments.application.usecase.exception.PaymentConflictException rejected) {
					return false;
				}
			});
			var secondPayment = executor.submit(() -> {
				await(gate);
				try {
					recordCollaboratorPayment.execute(context,
							new com.klaus.moply.payments.application.usecase.RecordCollaboratorPayment.Input(saved.id(),
									person, new BigDecimal("10.00"), LocalDate.of(2026, 9, 30), "concurrent-2",
									principal.getUserId()));
					return true;
				}
				catch (com.klaus.moply.payments.application.usecase.exception.PaymentConflictException expected) {
					return false;
				}
			});
			gate.countDown();
			assertEquals(1, (first.get(15, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0)
					+ (secondPayment.get(15, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0));
		}
		var totals = jdbc.queryForObject(
				"SELECT sum(amount) FROM tb_collaborator_payment WHERE organization_id=? AND work_order_id=? AND collaborator_id=? AND status='RECORDED'",
				java.math.BigDecimal.class, account, saved.id(), person);
		assertEquals(0, totals.compareTo(new BigDecimal("10.00")));

		var racingCancel = orders.save(account, work(LocalDate.of(2026, 9, 30), customer, "3", "11.50"));
		var race = new java.util.concurrent.CountDownLatch(1);
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			var posting = executor.submit(() -> {
				await(race);
				try {
					recordCollaboratorPayment.execute(context,
							new com.klaus.moply.payments.application.usecase.RecordCollaboratorPayment.Input(
									racingCancel.id(), person, new BigDecimal("10.00"), LocalDate.of(2026, 9, 30),
									"cancel-race", principal.getUserId()));
					return true;
				}
				catch (RuntimeException rejected) {
					return false;
				}
			});
			var cancelling = executor.submit(() -> {
				await(race);
				try {
					cancel.execute(context, new com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput(
							racingCancel.id(), principal.getUserId(), false, null));
					return true;
				}
				catch (RuntimeException rejected) {
					return false;
				}
			});
			race.countDown();
			posting.get(15, java.util.concurrent.TimeUnit.SECONDS);
			cancelling.get(15, java.util.concurrent.TimeUnit.SECONDS);
		}
		var afterRace = orders.findById(account, racingCancel.id()).orElseThrow();
		var active = jdbc.queryForObject(
				"SELECT count(*) FROM tb_collaborator_payment WHERE organization_id=? AND work_order_id=? AND status='RECORDED'",
				Integer.class, account, racingCancel.id());
		assertFalse(afterRace.status() == WorkOrderStatus.CANCELLED && active > 0);

		var rescheduling = orders.save(account, work(LocalDate.of(2026, 9, 30), customer, "3", "11.50"));
		var rescheduleGate = new java.util.concurrent.CountDownLatch(1);
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			var posting = executor.submit(() -> {
				await(rescheduleGate);
				try {
					return recordCollaboratorPayment.execute(context,
							new com.klaus.moply.payments.application.usecase.RecordCollaboratorPayment.Input(
									rescheduling.id(), person, new BigDecimal("5.00"), LocalDate.of(2026, 9, 30),
									"before-reschedule", principal.getUserId()));
				}
				catch (com.klaus.moply.payments.application.usecase.exception.PaymentConflictException expected) {
					return null;
				}
			});
			var moving = executor.submit(() -> {
				await(rescheduleGate);
				workflowReschedule.execute(context,
						new com.klaus.moply.workflows.application.usecase.dto.RescheduleWorkOrderInput(
								rescheduling.id(), LocalDate.of(2026, 10, 6), null));
			});
			rescheduleGate.countDown();
			posting.get(15, java.util.concurrent.TimeUnit.SECONDS);
			moving.get(15, java.util.concurrent.TimeUnit.SECONDS);
		}
		assertEquals(LocalDate.of(2026, 10, 6),
				orders.findById(account, rescheduling.id()).orElseThrow().serviceDate());
		var settled = jdbc.queryForObject(
				"SELECT coalesce(sum(amount),0) FROM tb_collaborator_payment WHERE organization_id=? AND work_order_id=? AND status='RECORDED'",
				BigDecimal.class, account, rescheduling.id());
		assertTrue(settled.signum() == 0 || settled.compareTo(new BigDecimal("5.00")) == 0);
	}

	private void assertSameState(WorkOrder expected, WorkOrder actual) {
		assertTrue(expected.hasSameConditionsAs(actual));
		assertTrue(expected.hasSameOperationAs(actual));
	}

}
