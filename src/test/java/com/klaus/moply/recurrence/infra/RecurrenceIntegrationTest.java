package com.klaus.moply.recurrence.infra;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.*;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.factory.PostgresSpringIntegrationTest;
import com.klaus.moply.payments.application.usecase.*;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.application.usecase.CreateSeries;
import com.klaus.moply.recurrence.application.usecase.FindSeries;
import com.klaus.moply.recurrence.application.usecase.GenerateSeries;
import com.klaus.moply.recurrence.application.usecase.dto.CreateSeriesInput;
import com.klaus.moply.recurrence.application.usecase.exception.SeriesNotFoundException;
import com.klaus.moply.recurrence.domain.*;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.workflows.application.CancelWorkOrder;
import com.klaus.moply.workflows.application.RescheduleWorkOrder;
import com.klaus.moply.workorders.application.ports.*;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = { "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate",
		"spring.jpa.open-in-view=false" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecurrenceIntegrationTest extends PostgresSpringIntegrationTest {

	@Autowired
	CreateSeries create;

	@Autowired
	GenerateSeries generate;

	@Autowired
	FindSeries find;

	@Autowired
	RecurrenceRepository seriesRepository;

	@Autowired
	WorkOrderRepository works;

	@Autowired
	OrganizationRepository accounts;

	@Autowired
	CustomerRepository customers;

	@Autowired
	CollaboratorRepository people;

	@Autowired
	CancelWorkOrder cancel;

	@Autowired
	RescheduleWorkOrder reschedule;

	@Autowired
	RecordCollaboratorPayment settle;

	@Autowired
	ReverseCollaboratorPayment reverseSettlement;

	@Autowired
	RecordWorkOrderPayment pay;

	@Autowired
	GetCollaboratorPaymentSummary summary;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MockMvc mvc;

	@MockitoBean
	Clock clock;

	@MockitoSpyBean
	CreateWorkOrder createWork;

	UUID account, customer, first, second, actor;

	Context context;

	final LocalDate today = LocalDate.of(2026, 10, 3);

	@BeforeEach
	void setup() {
		setTime("2026-10-03T12:00:00Z");
		account = UUID.randomUUID();
		actor = UUID.randomUUID();
		context = new Context(account);
		jdbc.update(
				"INSERT INTO tb_organization(id,name,currency_code,timezone,default_work_status) VALUES (?,'Recurring','GBP','UTC','SCHEDULED')",
				account);
		customer = customers.save(account, Customer.create("Client")).getId();
		first = people.save(account, Collaborator.create(account, "First", null)).getId();
		second = people.save(account, Collaborator.create(account, "Second", null)).getId();
	}

	void setTime(String instant) {
		var fixed = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
		when(clock.instant()).thenReturn(fixed.instant());
		doAnswer(i -> fixed.withZone(i.getArgument(0))).when(clock).withZone(any());
	}

	@AfterEach
	void cleanup() {
		reset(createWork);
		jdbc.update("DELETE FROM tb_collaborator_payment WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_customer_payment WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_work_assignment WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_order_service WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_recurrence_member WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_recurrence_series WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_customer WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_collaborator WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_organization WHERE id=?", account);
	}

	CreateSeriesInput input(LocalDate start, LocalDate end, WorkOrderStatus status) {
		return new CreateSeriesInput(Frequency.WEEKLY, start, end, new CreateWorkOrderInput(customer, null, start,
				LocalTime.NOON, "Repeat", BigDecimal.ONE, new BigDecimal("10.01"), List.of(second, first), status));
	}

	List<WorkOrder> all() {
		return works.findAll(account, new WorkOrderDateRange(null, null), null);
	}

	long count(String table) {
		return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE organization_id=?", Long.class, account);
	}

	AccountPrincipal principal() {
		return new AccountPrincipal(new AppUser(actor, account, new LoginEmail("owner@recurrence"), "unused"));
	}

	@Test
	void shouldCreateIndependentAssignmentsAndFreezeDefaultWithoutRecordingMoney() {
		accounts.update(accounts.findById(account).orElseThrow().withPreferences("UTC", DefaultWorkStatus.COMPLETED));
		var series = create.execute(context, input(today, null, null));
		assertEquals(5, all().size());
		assertEquals(List.of(second, first), find.execute(context, series.getId()).getTemplate().participants().ids());
		for (var work : all()) {
			assertEquals(WorkOrderStatus.COMPLETED, work.status());
			assertEquals(new BigDecimal("5.01"), work.assignments().getFirst().allocatedAmount().value());
			assertEquals(second, work.assignments().getFirst().collaboratorId());
		}
		assertEquals(10, jdbc.queryForObject(
				"SELECT count(DISTINCT id) FROM tb_work_assignment WHERE organization_id=?", Integer.class, account));
		assertEquals(0, count("tb_customer_payment"));
		assertEquals(0, count("tb_collaborator_payment"));
		var future = all().get(1);
		assertThrows(PaymentConflictException.class,
				() -> pay.execute(context, new RecordWorkOrderPayment.Input(future.id(), today, actor)));
		assertThrows(PaymentConflictException.class, () -> settle.execute(context,
				new RecordCollaboratorPayment.Input(future.id(), second, BigDecimal.ONE, today, "future", actor)));
		var balances = summary.execute(context, second);
		assertEquals(1, balances.workOrders().stream().filter(b -> b.requiresAttention()).count());
		accounts.update(accounts.findById(account).orElseThrow().withPreferences("UTC", DefaultWorkStatus.SCHEDULED));
		setTime("2026-10-10T12:00:00Z");
		assertEquals(1, generate.execute(context, series.getId()).created());
		assertTrue(all().stream().allMatch(w -> w.status() == WorkOrderStatus.COMPLETED));
	}

	@Test
	void shouldKeepOriginalIdentityAfterRescheduleAndCancellation() {
		var series = create.execute(context, input(today, null, null));
		var firstWork = all().getFirst();
		var next = all().get(1);
		cancel.execute(context, firstWork.id());
		reschedule.execute(context, new RescheduleWorkOrder.Input(next.id(), today.plusDays(70), null));
		var result = generate.execute(context, series.getId());
		assertEquals(0, result.created());
		assertEquals(5, result.existing());
		assertEquals(5, all().size());
		assertEquals(series.getId(), works.findById(account, next.id()).orElseThrow().occurrence().seriesId());
		assertEquals(today.plusDays(7), works.findById(account, next.id()).orElseThrow().occurrence().originalDate());
		assertEquals(today.plusDays(7), jdbc.queryForObject("SELECT occurrence_date FROM tb_order_service WHERE id=?",
				LocalDate.class, next.id()));
		works.findAll(account, new WorkOrderDateRange(today.plusDays(100), today.plusDays(200)), null);
		assertEquals(5, all().size());
	}

	@Test
	void shouldGenerateOnlyCurrentWindowAfterLongOutageAndUseAccountTimezone() {
		accounts.update(
				accounts.findById(account).orElseThrow().withPreferences("Europe/London", DefaultWorkStatus.SCHEDULED));
		setTime("2026-09-30T23:30:00Z");
		var series = create.execute(context, input(LocalDate.of(2026, 10, 1), null, null));
		assertEquals(LocalDate.of(2026, 10, 1), all().getFirst().serviceDate());
		setTime("2027-02-01T00:00:00Z");
		var result = generate.execute(context, series.getId());
		assertEquals(LocalDate.of(2027, 2, 1), result.from());
		assertTrue(all().stream()
			.noneMatch(w -> w.serviceDate().isAfter(LocalDate.of(2026, 10, 30))
					&& w.serviceDate().isBefore(LocalDate.of(2027, 2, 1))));
		assertTrue(all().stream().allMatch(w -> w.serviceDate().isBefore(LocalDate.of(2027, 3, 3))));
		assertEquals(0, generate.execute(context, series.getId()).created());
	}

	@Test
	void shouldSerializeConcurrentGenerationOnPostgres() throws Exception {
		var series = create.execute(context, input(today.plusDays(40), null, null));
		assertTrue(all().isEmpty());
		setTime("2026-11-12T12:00:00Z");
		var gate = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Callable<GenerateSeries.Result> task = () -> {
				assertTrue(gate.await(10, TimeUnit.SECONDS));
				return generate.execute(context, series.getId());
			};
			var a = executor.submit(task);
			var b = executor.submit(task);
			gate.countDown();
			assertEquals(5, a.get(20, TimeUnit.SECONDS).created() + b.get(20, TimeUnit.SECONDS).created());
		}
		assertEquals(5, all().size());
		assertEquals(5,
				jdbc.queryForObject(
						"SELECT count(DISTINCT occurrence_date) FROM tb_order_service WHERE recurrence_series_id=?",
						Integer.class, series.getId()));
	}

	@Test
	void shouldRollbackFailedSeriesAndContinueTheBatchThenRecover() {
		var bad = create.execute(context, input(today.plusDays(40), null, null));
		var badCustomer = customer;
		customer = customers.save(account, Customer.create("Healthy")).getId();
		var healthy = create.execute(context, input(today.plusDays(40), null, null));
		setTime("2026-11-12T12:00:00Z");
		doAnswer(i -> {
			var data = (CreateWorkOrderInput) i.getArgument(1);
			if (data.customerId().equals(badCustomer) && data.serviceDate().equals(LocalDate.of(2026, 11, 19)))
				throw new IllegalStateException("Injected second occurrence failure");
			return i.callRealMethod();
		}).when(createWork).execute(any(), any());
		new RecurrenceScheduler(seriesRepository, generate).run();
		assertEquals(5, all().size());
		assertTrue(all().stream().allMatch(w -> w.customerId().equals(customer)));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_order_service WHERE recurrence_series_id=?",
				Integer.class, bad.getId()));
		reset(createWork);
		assertEquals(5, generate.execute(context, bad.getId()).created());
		assertEquals(0, generate.execute(context, healthy.getId()).created());
	}

	@Test
	void shouldRollbackInitialCreationAndValidateEvenOutsideHorizon() {
		doAnswer(i -> {
			var data = (CreateWorkOrderInput) i.getArgument(1);
			if (data.serviceDate().equals(today.plusDays(7)))
				throw new IllegalStateException("Injected failure");
			return i.callRealMethod();
		}).when(createWork).execute(any(), any());
		assertThrows(IllegalStateException.class, () -> create.execute(context, input(today, null, null)));
		assertEquals(0, count("tb_recurrence_series"));
		assertEquals(0, count("tb_order_service"));
		assertEquals(0, count("tb_work_assignment"));
		reset(createWork);
		people.save(account, people.findById(account, first).orElseThrow().deactivate());
		assertThrows(com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException.class,
				() -> create.execute(context, input(today.plusDays(60), null, null)));
		assertEquals(0, count("tb_recurrence_series"));
	}

	@Test
	void shouldFailForDeactivatedMemberWithoutSilentlyRedistributing() {
		var series = create.execute(context, input(today.plusDays(40), null, null));
		people.save(account, people.findById(account, first).orElseThrow().deactivate());
		setTime("2026-11-12T12:00:00Z");
		assertThrows(com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException.class,
				() -> generate.execute(context, series.getId()));
		assertTrue(all().isEmpty());
		assertEquals(List.of(second, first), find.execute(context, series.getId()).getTemplate().participants().ids());
	}

	@Test
	void shouldPreserveFinancialContractsAndNeverCopySettlementsToNextOccurrence() {
		var series = create.execute(context, input(today, null, null));
		var work = all().getFirst();
		var one = settle.execute(context,
				new RecordCollaboratorPayment.Input(work.id(), second, new BigDecimal("2.00"), today, "part1", actor));
		settle.execute(context,
				new RecordCollaboratorPayment.Input(work.id(), second, new BigDecimal("3.01"), today, "part2", actor));
		assertEquals(2, count("tb_collaborator_payment"));
		assertThrows(PaymentConflictException.class,
				() -> settle.execute(context, new RecordCollaboratorPayment.Input(work.id(), second,
						new BigDecimal("0.01"), today, "excess", actor)));
		assertThrows(PaymentConflictException.class, () -> cancel.execute(context, work.id()));
		reschedule.execute(context, new RescheduleWorkOrder.Input(work.id(), today.plusDays(2), null));
		assertEquals(one.id(),
				settle
					.execute(context,
							new RecordCollaboratorPayment.Input(work.id(), second, new BigDecimal("2.00"), today,
									"part1", actor))
					.id());
		reverseSettlement.execute(context,
				new ReverseCollaboratorPayment.Input(work.id(), second, one.id(), true, "Not delivered", actor));
		setTime("2026-10-10T12:00:00Z");
		generate.execute(context, series.getId());
		assertEquals(2, count("tb_collaborator_payment"));
		assertEquals(0, count("tb_customer_payment"));
		assertEquals(1,
				jdbc.queryForObject(
						"SELECT count(DISTINCT work_order_id) FROM tb_collaborator_payment WHERE organization_id=?",
						Integer.class, account));
		var next = all().stream().filter(w -> w.serviceDate().equals(today.plusDays(7))).findFirst().orElseThrow();
		pay.execute(context, new RecordWorkOrderPayment.Input(next.id(), today.plusDays(7), actor));
		assertThrows(PaymentConflictException.class,
				() -> reschedule.execute(context, new RescheduleWorkOrder.Input(next.id(), today.plusDays(8), null)));
		assertEquals(1, count("tb_customer_payment"));
	}

	@Test
	void shouldProtectApiAndTenantScope() throws Exception {
		var body = """
				{"frequency":"WEEKLY","startsOn":"2026-10-03","endsOn":"2026-10-10", "customerId":"%s", "contractedHours":1,"hourlyRate":10,"participantIds":["%s","%s"]}
				"""
			.formatted(customer, second, first);
		mvc.perform(
				post("/api/v1/recurrence-series").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/recurrence-series").with(user(principal()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)).andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/recurrence-series").with(user(principal()))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.conditions.initialStatus").value("SCHEDULED"))
			.andExpect(jsonPath("$.conditions.contractedHours").value(1))
			.andExpect(jsonPath("$.conditions.hourlyRate").value(10))
			.andExpect(jsonPath("$.conditions.participantIds[0]").value(second.toString()))
			.andExpect(jsonPath("$.conditions.participantIds[1]").value(first.toString()))
			.andExpect(jsonPath("$.conditions.participants").doesNotExist());
		var id = jdbc.queryForObject("SELECT id FROM tb_recurrence_series WHERE organization_id=?", UUID.class,
				account);
		assertEquals(2, all().size());
		var foreignContext = new Context(UUID.randomUUID());
		assertThrows(SeriesNotFoundException.class, () -> find.execute(foreignContext, id));
		assertThrows(SeriesNotFoundException.class, () -> generate.execute(foreignContext, id));
		var foreign = new AccountPrincipal(new AppUser(UUID.randomUUID(), foreignContext.organizationId(),
				new LoginEmail("other@recurrence"), "unused"));
		mvc.perform(get("/api/v1/recurrence-series/" + id).with(user(foreign))).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/recurrence-series/" + id).with(user(principal())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.conditions.participantIds[0]").value(second.toString()))
			.andExpect(jsonPath("$.conditions.participantIds[1]").value(first.toString()));
		mvc.perform(post("/api/v1/recurrence-series").with(user(principal()))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body.replace(customer.toString(), UUID.randomUUID().toString()))).andExpect(status().isNotFound());
	}

}
