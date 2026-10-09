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

import com.klaus.moply.payments.application.usecase.dto.RecordCollaboratorPaymentInput;
import com.klaus.moply.payments.application.usecase.dto.RecordWorkOrderPaymentInput;
import com.klaus.moply.payments.application.usecase.dto.ReverseCollaboratorPaymentInput;
import com.klaus.moply.payments.application.usecase.dto.ReverseWorkOrderPaymentInput;
import com.klaus.moply.recurrence.application.usecase.dto.FindOccurrenceHistoryFilter;
import com.klaus.moply.recurrence.application.usecase.dto.GenerateSeriesResult;
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
import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.application.usecase.CreateSeries;
import com.klaus.moply.recurrence.application.usecase.FindOccurrenceHistory;
import com.klaus.moply.recurrence.application.usecase.FindSeries;
import com.klaus.moply.recurrence.application.usecase.GenerateSeries;
import com.klaus.moply.recurrence.application.usecase.dto.CreateSeriesInput;
import com.klaus.moply.recurrence.application.usecase.exception.SeriesNotFoundException;
import com.klaus.moply.recurrence.domain.*;
import com.klaus.moply.recurrence.infra.scheduler.RecurrenceScheduler;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.*;
import com.klaus.moply.workflows.application.usecase.dto.*;
import com.klaus.moply.workorders.application.ports.*;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;
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
	CancelRecurringWork cancelRecurring;

	@Autowired
	RescheduleRecurringWork rescheduleRecurring;

	@Autowired
	FindOccurrenceHistory history;

	@Autowired
	WorkOrderOperations operations;

	@Autowired
	org.springframework.transaction.PlatformTransactionManager transactions;

	@Autowired
	com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository paymentRepository;

	@MockitoSpyBean
	RecurrenceChanges changes;

	@Autowired
	com.klaus.moply.workorders.application.usecase.PreviewWorkOrderPricing pricingPreview;

	@Autowired
	CreateSeries create;

	@Autowired
	GenerateSeries generate;

	@Autowired
	FindSeries find;

	@MockitoSpyBean
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
	ReverseWorkOrderPayment reversePayment;

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

	private FindOccurrenceHistoryFilter historyFilter(UUID workOrderId) {
		return new FindOccurrenceHistoryFilter(workOrderId,
				com.klaus.moply.shared.application.pagination.PageQuery.defaults());
	}

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
		reset(createWork, seriesRepository, changes);
		jdbc.update("DELETE FROM tb_recurrence_change_item WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_recurrence_command WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_recurrence_exclusion WHERE organization_id=?", account);
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
	void shouldFreezeApprovedMixedRatesAcrossGenerationAndSuccessorSeries() {
		people.save(account, people.findById(account, first).orElseThrow().update("First", null, new BigDecimal("20")));
		var request = new CreateWorkOrderInput(customer, null, today, LocalTime.NOON, "Mixed", new BigDecimal("4"),
				new BigDecimal("30"), List.of(first, second), null);
		var preview = pricingPreview.execute(context, request);
		var accepted = new CreateWorkOrderInput(customer, null, today, LocalTime.NOON, "Mixed", new BigDecimal("4"),
				new BigDecimal("30"), List.of(first, second), null, preview.pricingFingerprint());
		assertThrows(com.klaus.moply.workorders.application.usecase.exception.PricingAcceptanceException.class,
				() -> create.execute(context, new CreateSeriesInput(Frequency.WEEKLY, today, null, request)));
		assertEquals(0, count("tb_recurrence_series"));
		var series = create.execute(context, new CreateSeriesInput(Frequency.WEEKLY, today, null, accepted));
		var frozen = find.execute(context, series.getId()).getTemplate().frozenPricing();
		assertEquals(new BigDecimal("50.00"), frozen.assignments().values().getFirst().allocatedAmount().value());
		people.save(account,
				people.findById(account, first).orElseThrow().update("First", null, new BigDecimal("100")));
		accounts.updatePreferences(account,
				organization -> organization.withPreferences("UTC", DefaultWorkStatus.COMPLETED, new BigDecimal("99")));
		setTime("2026-11-12T12:00:00Z");
		assertTrue(generate.execute(context, series.getId()).created() > 0);
		for (var work : all()) {
			assertEquals(new BigDecimal("120.00"), work.totalAmount().value());
			assertEquals(new BigDecimal("50.00"), work.assignments().getFirst().allocatedAmount().value());
			assertEquals(new BigDecimal("20.00"), work.assignments().getFirst().appliedHourlyRate().value());
		}
		var successor = series.successor(10, today.plusDays(70), LocalTime.NOON);
		seriesRepository.save(successor);
		assertEquals(frozen, find.execute(context, successor.getId()).getTemplate().frozenPricing());
	}

	@Test
	void shouldPreserveLegacyRecurringRateAfterAddingCollaboratorFixedRate() {
		var series = create.execute(context, input(today, null, null));
		// Simulate a historical series that has no financial snapshot.
		jdbc.update("UPDATE tb_recurrence_series SET total_amount=NULL,allocation_policy_version=NULL WHERE id=?",
				series.getId());
		jdbc.update(
				"UPDATE tb_recurrence_member SET allocated_amount=NULL,applied_hourly_rate=NULL,fixed_rate=NULL,base_amount=NULL,surplus_amount=NULL WHERE series_id=?",
				series.getId());
		people.save(account,
				people.findById(account, second).orElseThrow().update("Second", null, new BigDecimal("100")));
		setTime("2026-11-12T12:00:00Z");
		assertTrue(generate.execute(context, series.getId()).created() > 0);
		for (var work : all()) {
			assertEquals(1, work.allocationPolicyVersion());
			assertEquals(new BigDecimal("5.01"), work.assignments().getFirst().allocatedAmount().value());
		}
	}

	@Test
	void shouldCreateIndependentAssignmentsAndFreezeDefaultWithoutRecordingMoney() {
		accounts.updatePreferences(account,
				organization -> organization.withPreferences("UTC", DefaultWorkStatus.COMPLETED));
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
				() -> pay.execute(context, new RecordWorkOrderPaymentInput(future.id(), today, actor)));
		assertThrows(PaymentConflictException.class, () -> settle.execute(context,
				new RecordCollaboratorPaymentInput(future.id(), second, BigDecimal.ONE, today, "future", actor)));
		var balances = summary.execute(context, second);
		assertEquals(1, balances.workOrders().stream().filter(b -> b.requiresAttention()).count());
		accounts.updatePreferences(account,
				organization -> organization.withPreferences("UTC", DefaultWorkStatus.SCHEDULED));
		setTime("2026-10-10T12:00:00Z");
		assertEquals(1, generate.execute(context, series.getId()).created());
		assertTrue(all().stream().allMatch(w -> w.status() == WorkOrderStatus.COMPLETED));
	}

	@Test
	void shouldKeepOriginalIdentityAfterRescheduleAndCancellation() {
		var series = create.execute(context, input(today, null, null));
		var firstWork = all().getFirst();
		var next = all().get(1);
		cancel.execute(context, new CancelWorkOrderInput(firstWork.id(), null, false, null));
		reschedule.execute(context, new RescheduleWorkOrderInput(next.id(), today.plusDays(70), null));
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
		accounts.updatePreferences(account,
				organization -> organization.withPreferences("Europe/London", DefaultWorkStatus.SCHEDULED));
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
			Callable<GenerateSeriesResult> task = () -> {
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
		}).when(createWork).executeFrozen(any(), any(), any(), any());
		new RecurrenceScheduler(seriesRepository, generate).run();
		assertEquals(5, all().size());
		assertTrue(all().stream().allMatch(w -> w.customerId().equals(customer)));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_order_service WHERE recurrence_series_id=?",
				Integer.class, bad.getId()));
		reset(createWork, seriesRepository, changes);
		jdbc.update("DELETE FROM tb_recurrence_change_item WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_recurrence_command WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_recurrence_exclusion WHERE organization_id=?", account);
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
		}).when(createWork).executeFrozen(any(), any(), any(), any());
		assertThrows(IllegalStateException.class, () -> create.execute(context, input(today, null, null)));
		assertEquals(0, count("tb_recurrence_series"));
		assertEquals(0, count("tb_order_service"));
		assertEquals(0, count("tb_work_assignment"));
		reset(createWork, seriesRepository, changes);
		jdbc.update("DELETE FROM tb_recurrence_change_item WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_recurrence_command WHERE organization_id=?", account);
		jdbc.update("DELETE FROM tb_recurrence_exclusion WHERE organization_id=?", account);
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
				new RecordCollaboratorPaymentInput(work.id(), second, new BigDecimal("2.00"), today, "part1", actor));
		settle.execute(context,
				new RecordCollaboratorPaymentInput(work.id(), second, new BigDecimal("3.01"), today, "part2", actor));
		assertEquals(2, count("tb_collaborator_payment"));
		assertThrows(PaymentConflictException.class, () -> settle.execute(context,
				new RecordCollaboratorPaymentInput(work.id(), second, new BigDecimal("0.01"), today, "excess", actor)));
		assertThrows(PaymentConflictException.class,
				() -> cancel.execute(context, new CancelWorkOrderInput(work.id(), null, false, null)));
		reschedule.execute(context, new RescheduleWorkOrderInput(work.id(), today.plusDays(2), null));
		assertEquals(one.id(),
				settle
					.execute(context,
							new RecordCollaboratorPaymentInput(work.id(), second, new BigDecimal("2.00"), today,
									"part1", actor))
					.id());
		reverseSettlement.execute(context,
				new ReverseCollaboratorPaymentInput(work.id(), second, one.id(), true, "Not delivered", actor));
		setTime("2026-10-10T12:00:00Z");
		generate.execute(context, series.getId());
		assertEquals(2, count("tb_collaborator_payment"));
		assertEquals(0, count("tb_customer_payment"));
		assertEquals(1,
				jdbc.queryForObject(
						"SELECT count(DISTINCT work_order_id) FROM tb_collaborator_payment WHERE organization_id=?",
						Integer.class, account));
		var next = all().stream().filter(w -> w.serviceDate().equals(today.plusDays(7))).findFirst().orElseThrow();
		pay.execute(context, new RecordWorkOrderPaymentInput(next.id(), today.plusDays(7), actor));
		assertThrows(PaymentConflictException.class,
				() -> reschedule.execute(context, new RescheduleWorkOrderInput(next.id(), today.plusDays(8), null)));
		assertEquals(1, count("tb_customer_payment"));
	}

	@Test
	void shouldProtectApiAndTenantScope() throws Exception {
		var body = """
				{"frequency":"WEEKLY","period":{"startsOn":"2026-10-03","endsOn":"2026-10-10"}, "conditions":{"customerId":"%s", "contractedHours":1,"hourlyRate":10,"participantIds":["%s","%s"]}}
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
			.andExpect(jsonPath("$.conditions.pricing.contractedHours").value(1))
			.andExpect(jsonPath("$.conditions.pricing.hourlyRate").value(10))
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

	OccurrenceSelection selection(UUID id, ChangeScope scope, String key) {
		return new OccurrenceSelection(id, actor, scope, key);
	}

	void move(UUID id, ChangeScope scope, String key, LocalDate date) {
		rescheduleRecurring.execute(context,
				new RescheduleOccurrenceInput(selection(id, scope, key), date, LocalTime.NOON));
	}

	void stop(UUID id, ChangeScope scope, String key) {
		cancelRecurring.execute(context, new CancelOccurrenceInput(selection(id, scope, key), List.of()));
	}

	UUID successor(String key) {
		return jdbc.queryForObject(
				"SELECT successor_id FROM tb_recurrence_command WHERE organization_id=? AND command_key=?", UUID.class,
				account, key);
	}

	List<WorkOrder> active() {
		return all().stream().filter(w -> w.status() != WorkOrderStatus.CANCELLED).toList();
	}

	@Test
	void shouldCutByOriginalPositionAfterIsolatedMoveAndKeepEarlierWork() {
		var series = create.execute(context, input(today, null, null));
		var original = all();
		var cut = original.get(2);
		move(cut.id(), ChangeScope.THIS_OCCURRENCE, "single", today.plusDays(70));
		stop(cut.id(), ChangeScope.THIS_AND_FOLLOWING, "stop");
		assertEquals(original.subList(0, 2).stream().map(WorkOrder::id).toList(),
				active().stream().map(WorkOrder::id).toList());
		assertEquals(today.plusDays(70), works.findById(account, cut.id()).orElseThrow().serviceDate());
		assertEquals(cut.occurrence(), works.findById(account, cut.id()).orElseThrow().occurrence());
		setTime("2026-12-01T12:00:00Z");
		generate.execute(context, series.getId());
		assertEquals(5, all().size());
		stop(cut.id(), ChangeScope.THIS_AND_FOLLOWING, "stop");
		assertEquals(2, count("tb_recurrence_command"));
	}

	@Test
	void shouldReplaceAndTraceNewCalendarWithoutMovingAssignmentsOrMoney() {
		var series = create.execute(context, input(today, today.plusDays(28), null));
		var old = all();
		move(old.get(1).id(), ChangeScope.THIS_AND_FOLLOWING, "split", today.plusDays(9));
		var next = find.execute(context, successor("split"));
		assertEquals(series.getId(), next.getLineage().familyId());
		assertEquals(series.getId(), next.getLineage().previousSeriesId());
		assertEquals(today.plusDays(28), next.getPeriod().endsOn());
		assertEquals(List.of(today, today.plusDays(9), today.plusDays(16), today.plusDays(23)),
				active().stream().map(WorkOrder::serviceDate).toList());
		var audit = history.execute(context, historyFilter(old.get(1).id())).getFirst();
		assertEquals("REPLACED", audit.reason());
		assertEquals(actor, audit.actorId());
		assertEquals(next.getId(), audit.targetSeriesId());
		assertNotNull(audit.replacementWorkId());
		assertEquals(old.get(1).assignments(), works.findById(account, old.get(1).id()).orElseThrow().assignments());
		assertEquals(0, count("tb_customer_payment"));
		assertEquals(0, count("tb_collaborator_payment"));
		long size = all().size();
		move(old.get(1).id(), ChangeScope.THIS_AND_FOLLOWING, "split", today.plusDays(9));
		generate.execute(context, series.getId());
		generate.execute(context, next.getId());
		assertEquals(size, all().size());
		assertEquals(2, count("tb_recurrence_series"));
		assertThrows(WorkOrderStateException.class,
				() -> move(old.get(1).id(), ChangeScope.THIS_AND_FOLLOWING, "split", today.plusDays(10)));
	}

	@Test
	void shouldCarryIndividualExclusionsAcrossVersionsAndDelayedGeneration() {
		var original = create.execute(context, input(today, null, null));
		var old = all();
		stop(old.get(2).id(), ChangeScope.THIS_OCCURRENCE, "exclude");
		move(old.get(1).id(), ChangeScope.THIS_AND_FOLLOWING, "split", today.plusDays(70));
		assertEquals(1, active().size());
		setTime("2026-12-12T12:00:00Z");
		generate.execute(context, successor("split"));
		assertFalse(active().stream().anyMatch(w -> w.serviceDate().equals(today.plusDays(77))));
		assertTrue(active().stream().anyMatch(w -> w.serviceDate().equals(today.plusDays(70))));
		generate.execute(context, original.getId());
		assertEquals(1, count("tb_recurrence_exclusion"));
		assertNull(history.execute(context, historyFilter(old.get(2).id())).getFirst().targetSeriesId());
	}

	@Test
	void shouldReachSuccessorsWhenCommandStartsInHistoricalVersion() {
		var original = create.execute(context, input(today, null, null));
		var old = all();
		move(old.get(2).id(), ChangeScope.THIS_AND_FOLLOWING, "split", today.plusDays(15));
		move(old.get(1).id(), ChangeScope.THIS_AND_FOLLOWING, "split-again", today.plusDays(8));
		assertEquals(3, count("tb_recurrence_series"));
		assertEquals(List.of(today, today.plusDays(8), today.plusDays(15), today.plusDays(22), today.plusDays(29)),
				active().stream().map(WorkOrder::serviceDate).toList());
		assertEquals(2L, find.execute(context, successor("split")).getLineage().untilPosition());
		stop(old.get(1).id(), ChangeScope.THIS_AND_FOLLOWING, "end-family");
		for (var id : List.of(original.getId(), successor("split"), successor("split-again")))
			generate.execute(context, id);
		assertEquals(List.of(old.getFirst().id()), active().stream().map(WorkOrder::id).toList());
	}

	@Test
	void shouldRequireEachPaymentConfirmationAndReverseOnlyOnce() {
		create.execute(context, input(today, null, null));
		var old = all();
		setTime("2026-10-31T12:00:00Z");
		var p1 = pay.execute(context, new RecordWorkOrderPaymentInput(old.getFirst().id(), today, actor));
		var p2 = pay.execute(context, new RecordWorkOrderPaymentInput(old.get(1).id(), today.plusDays(7), actor));
		var c1 = new PaymentConfirmation(p1.id(), true, "Incorrect entry");
		var c2 = new PaymentConfirmation(p2.id(), true, "Not received");
		var selection = selection(old.getFirst().id(), ChangeScope.THIS_AND_FOLLOWING, "cancel-paid");
		assertThrows(PaymentConflictException.class,
				() -> cancelRecurring.execute(context, new CancelOccurrenceInput(selection, List.of(c1))));
		assertEquals(5, active().size());
		assertEquals(0, count("tb_recurrence_command"));
		assertTrue(paymentRepository.findActiveByWork(account, old.getFirst().id()).isPresent());
		var input = new CancelOccurrenceInput(selection, List.of(c1, c2));
		cancelRecurring.execute(context, input);
		cancelRecurring.execute(context, input);
		assertTrue(active().isEmpty());
		assertEquals(2, count("tb_customer_payment"));
		assertEquals(2,
				jdbc.queryForObject(
						"SELECT count(*) FROM tb_customer_payment WHERE organization_id=? AND status='REVERSED'",
						Integer.class, account));
		assertEquals(1, count("tb_recurrence_command"));
	}

	@Test
	void shouldBlockEntireBatchOnActiveSettlementButPreserveItOnSingleMove() {
		create.execute(context, input(today, null, null));
		var old = all();
		var payment = settle.execute(context, new RecordCollaboratorPaymentInput(old.getFirst().id(), second,
				new BigDecimal("2.00"), today, "partial", actor));
		assertThrows(PaymentConflictException.class,
				() -> stop(old.getFirst().id(), ChangeScope.THIS_AND_FOLLOWING, "stop"));
		assertThrows(PaymentConflictException.class,
				() -> move(old.getFirst().id(), ChangeScope.THIS_AND_FOLLOWING, "move", today.plusDays(1)));
		assertEquals(5, active().size());
		assertEquals(1, count("tb_recurrence_series"));
		assertEquals(0, count("tb_recurrence_command"));
		move(old.getFirst().id(), ChangeScope.THIS_OCCURRENCE, "single", today.plusDays(1));
		assertEquals(1, count("tb_collaborator_payment"));
		reverseSettlement.execute(context, new ReverseCollaboratorPaymentInput(old.getFirst().id(), second,
				payment.id(), true, "Not delivered", actor));
		move(old.getFirst().id(), ChangeScope.THIS_AND_FOLLOWING, "move", today.plusDays(2));
		assertEquals(1, count("tb_collaborator_payment"));
		assertEquals("REVERSED", jdbc.queryForObject("SELECT status FROM tb_collaborator_payment WHERE id=?",
				String.class, payment.id()));
	}

	@Test
	void shouldRejectPaidAndReachedCompletedWorksButAllowFutureCompleted() {
		create.execute(context, input(today, null, WorkOrderStatus.COMPLETED));
		var old = all();
		assertThrows(WorkOrderStateException.class,
				() -> move(old.getFirst().id(), ChangeScope.THIS_AND_FOLLOWING, "past", today.plusDays(1)));
		move(old.get(1).id(), ChangeScope.THIS_AND_FOLLOWING, "future", today.plusDays(8));
		assertTrue(active().stream().allMatch(w -> w.status() == WorkOrderStatus.COMPLETED));
		assertEquals(1, count("tb_recurrence_command"));
	}

	@Test
	void shouldRollbackClosureReplacementsAndAuditWhenSuccessorGenerationFails() {
		var original = create.execute(context, input(today, null, null));
		var old = all();
		doThrow(new IllegalStateException("injected generation failure")).when(createWork)
			.executeFrozen(any(), any(), any(), any());
		assertThrows(IllegalStateException.class,
				() -> move(old.getFirst().id(), ChangeScope.THIS_AND_FOLLOWING, "fail", today.plusDays(1)));
		assertEquals(5, active().size());
		assertEquals(1, count("tb_recurrence_series"));
		assertEquals(0, count("tb_recurrence_command"));
		assertNull(find.execute(context, original.getId()).getLineage().untilPosition());
		reset(createWork);
		move(old.getFirst().id(), ChangeScope.THIS_AND_FOLLOWING, "fail", today.plusDays(1));
		assertEquals(2, count("tb_recurrence_series"));
	}

	@Test
	void shouldRollbackPaymentReversalWhenLaterCancellationFails() {
		create.execute(context, input(today, null, null));
		var old = all();
		var payment = pay.execute(context, new RecordWorkOrderPaymentInput(old.getFirst().id(), today, actor));
		doThrow(new org.springframework.dao.DataIntegrityViolationException("audit failure"))
			.when(org.springframework.test.util.AopTestUtils.<RecurrenceChanges>getUltimateTargetObject(changes))
			.record(argThat(item -> item.work().equals(old.getFirst().id())));
		var input = new CancelOccurrenceInput(selection(old.getFirst().id(), ChangeScope.THIS_AND_FOLLOWING, "failure"),
				List.of(new PaymentConfirmation(payment.id(), true, "Not received")));
		assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
				() -> cancelRecurring.execute(context, input));
		assertEquals(5, active().size());
		assertEquals(0, count("tb_recurrence_command"));
		assertTrue(paymentRepository.findActiveByWork(account, old.getFirst().id()).isPresent());
	}

	@Test
	void shouldKeepInclusiveEndAndMonthlyAnchorAfterSplit() {
		var start = LocalDate.of(2026, 10, 31);
		create.execute(context, new CreateSeriesInput(Frequency.MONTHLY, start, LocalDate.of(2027, 3, 31),
				input(start, null, null).work()));
		var work = all().getFirst();
		move(work.id(), ChangeScope.THIS_AND_FOLLOWING, "month", LocalDate.of(2026, 10, 30));
		setTime("2027-02-01T12:00:00Z");
		generate.execute(context, successor("month"));
		assertTrue(active().stream().anyMatch(w -> w.serviceDate().equals(LocalDate.of(2027, 2, 28))));
		setTime("2027-03-02T12:00:00Z");
		generate.execute(context, successor("month"));
		assertTrue(active().stream().anyMatch(w -> w.serviceDate().equals(LocalDate.of(2027, 3, 30))));
		assertThrows(DomainException.class,
				() -> move(work.id(), ChangeScope.THIS_AND_FOLLOWING, "invalid-end", LocalDate.of(2027, 4, 1)));
	}

	@Test
	void shouldEnforceRecurringScopeCsrfTenantAndAuthenticatedActor() throws Exception {
		create.execute(context, input(today, null, null));
		var id = all().getFirst().id();
		String path = "/api/v1/work-orders/" + id + "/cancel";
		String body = "{\"scope\":\"THIS_OCCURRENCE\",\"idempotencyKey\":\"web\",\"actorId\":\"" + UUID.randomUUID()
				+ "\"}";
		mvc.perform(post(path).with(user(principal())).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isForbidden());
		mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isUnauthorized());
		mvc.perform(post(path).with(user(principal())).with(csrf())).andExpect(status().isBadRequest());
		var foreign = new AccountPrincipal(
				new AppUser(UUID.randomUUID(), UUID.randomUUID(), new LoginEmail("foreign@test"), "unused"));
		mvc.perform(post(path).with(user(foreign)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isNotFound());
		mvc.perform(
				post(path).with(user(principal())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/work-orders/" + id + "/recurrence-history").with(user(principal())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].actorId").value(actor.toString()))
			.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/work-orders/" + id + "/recurrence-history").with(user(principal()))
			.param("page", "1")
			.param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/work-orders/" + id + "/recurrence-history").with(user(foreign)))
			.andExpect(status().isNotFound());
	}

	@Test
	void shouldSerializeDuplicateSplitWithGeneratorInPostgres() throws Exception {
		var series = create.execute(context, input(today, null, null));
		var id = all().getFirst().id();
		try (var pool = Executors.newFixedThreadPool(3)) {
			var start = new CyclicBarrier(3);
			var one = pool.submit(() -> {
				start.await();
				move(id, ChangeScope.THIS_AND_FOLLOWING, "same", today.plusDays(1));
				return true;
			});
			var two = pool.submit(() -> {
				start.await();
				move(id, ChangeScope.THIS_AND_FOLLOWING, "same", today.plusDays(1));
				return true;
			});
			var job = pool.submit(() -> {
				start.await();
				generate.execute(context, series.getId());
				return true;
			});
			assertTrue(one.get(15, TimeUnit.SECONDS));
			assertTrue(two.get(15, TimeUnit.SECONDS));
			assertTrue(job.get(15, TimeUnit.SECONDS));
		}
		assertEquals(2, count("tb_recurrence_series"));
		assertEquals(1, count("tb_recurrence_command"));
		assertEquals(5, active().size());
		assertEquals(10, all().size());
	}

	@Test
	void shouldRevalidatePaymentCommittedWhileCollectiveWaitsForWorkLock() throws Exception {
		create.execute(context, input(today, null, null));
		var id = all().getFirst().id();
		var workLocked = new CountDownLatch(1);
		var familyLocked = new CountDownLatch(1);
		doAnswer(invocation -> {
			var result = invocation.callRealMethod();
			familyLocked.countDown();
			return result;
		}).when(org.springframework.test.util.AopTestUtils
			.<RecurrenceRepository>getUltimateTargetObject(seriesRepository)).lockFamily(any(), any());
		try (var pool = Executors.newFixedThreadPool(2)) {
			var financial = pool
				.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactions)
					.execute(status -> operations.withWorkOrder(account, id, w -> {
						workLocked.countDown();
						try {
							assertTrue(familyLocked.await(10, TimeUnit.SECONDS));
						}
						catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
						return pay.execute(context, new RecordWorkOrderPaymentInput(id, today, actor));
					})));
			assertTrue(workLocked.await(10, TimeUnit.SECONDS));
			var collective = pool.submit(() -> assertThrows(PaymentConflictException.class,
					() -> move(id, ChangeScope.THIS_AND_FOLLOWING, "race", today.plusDays(1))));
			assertNotNull(financial.get(15, TimeUnit.SECONDS));
			assertNotNull(collective.get(15, TimeUnit.SECONDS));
		}
		assertEquals(5, active().size());
		assertEquals(1, count("tb_recurrence_series"));
		assertEquals(0, count("tb_recurrence_command"));
	}

	@Test
	void shouldRevalidateSettlementCommittedBeforeCollectiveCancellation() throws Exception {
		create.execute(context, input(today, null, null));
		var id = all().getFirst().id();
		var locked = new CountDownLatch(1);
		var family = new CountDownLatch(1);
		doAnswer(i -> {
			var r = i.callRealMethod();
			family.countDown();
			return r;
		}).when(org.springframework.test.util.AopTestUtils
			.<RecurrenceRepository>getUltimateTargetObject(seriesRepository)).lockFamily(any(), any());
		try (var pool = Executors.newFixedThreadPool(2)) {
			var financial = pool
				.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactions)
					.execute(status -> operations.withWorkOrder(account, id, w -> {
						locked.countDown();
						try {
							assertTrue(family.await(10, TimeUnit.SECONDS));
						}
						catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						return settle.execute(context, new RecordCollaboratorPaymentInput(id, second,
								new BigDecimal("2.00"), today, "race-settlement", actor));
					})));
			assertTrue(locked.await(10, TimeUnit.SECONDS));
			var collective = pool.submit(() -> assertThrows(PaymentConflictException.class,
					() -> stop(id, ChangeScope.THIS_AND_FOLLOWING, "race")));
			assertNotNull(financial.get(15, TimeUnit.SECONDS));
			assertNotNull(collective.get(15, TimeUnit.SECONDS));
		}
		assertEquals(5, active().size());
		assertEquals(1, count("tb_collaborator_payment"));
		assertEquals(0, count("tb_recurrence_command"));
	}

	@Test
	void shouldBlockFinancialCommandsUntilCollectiveCancellationCommits() throws Exception {
		create.execute(context, input(today, null, null));
		var id = all().getFirst().id();
		var changed = new CountDownLatch(1);
		var financialStarted = new CountDownLatch(2);
		doAnswer(i -> {
			var result = i.callRealMethod();
			changed.countDown();
			assertTrue(financialStarted.await(10, TimeUnit.SECONDS));
			return result;
		}).when(org.springframework.test.util.AopTestUtils.<RecurrenceChanges>getUltimateTargetObject(changes))
			.record(any());
		try (var pool = Executors.newFixedThreadPool(3)) {
			var collective = pool.submit(() -> {
				stop(id, ChangeScope.THIS_AND_FOLLOWING, "stop");
				return true;
			});
			assertTrue(changed.await(10, TimeUnit.SECONDS));
			var payment = pool.submit(() -> {
				financialStarted.countDown();
				return assertThrows(PaymentConflictException.class,
						() -> pay.execute(context, new RecordWorkOrderPaymentInput(id, today, actor)));
			});
			var settlement = pool.submit(() -> {
				financialStarted.countDown();
				return assertThrows(PaymentConflictException.class, () -> settle.execute(context,
						new RecordCollaboratorPaymentInput(id, second, new BigDecimal("1.00"), today, "late", actor)));
			});
			assertTrue(collective.get(15, TimeUnit.SECONDS));
			assertNotNull(payment.get(15, TimeUnit.SECONDS));
			assertNotNull(settlement.get(15, TimeUnit.SECONDS));
		}
		assertEquals(0, count("tb_customer_payment"));
		assertEquals(0, count("tb_collaborator_payment"));
		assertTrue(active().isEmpty());
	}

	@Test
	void shouldRejectStaleConfirmationAfterIndependentReversalAndNewPayment() {
		create.execute(context, input(today, null, null));
		var id = all().getFirst().id();
		var firstPayment = pay.execute(context, new RecordWorkOrderPaymentInput(id, today, actor));
		new org.springframework.transaction.support.TransactionTemplate(transactions)
			.executeWithoutResult(tx -> operations.withWorkOrder(account, id, w -> {
				paymentRepository.update(paymentRepository.findActiveByWork(account, id)
					.orElseThrow()
					.reverse(actor, "Incorrect", clock.instant()));
				return null;
			}));
		var next = pay.execute(context, new RecordWorkOrderPaymentInput(id, today, actor));
		var input = new CancelOccurrenceInput(selection(id, ChangeScope.THIS_AND_FOLLOWING, "stale"),
				List.of(new PaymentConfirmation(firstPayment.id(), true, "Not received")));
		assertThrows(PaymentConflictException.class, () -> cancelRecurring.execute(context, input));
		assertEquals(next.id(), paymentRepository.findActiveByWork(account, id).orElseThrow().id());
		assertEquals(5, active().size());
		assertEquals(0, count("tb_recurrence_command"));
	}

	@Test
	void shouldKeepIsolatedReschedulePaymentRestrictionAndRepeatedAuditStable() {
		create.execute(context, input(today, null, null));
		var id = all().getFirst().id();
		pay.execute(context, new RecordWorkOrderPaymentInput(id, today, actor));
		assertThrows(PaymentConflictException.class,
				() -> move(id, ChangeScope.THIS_OCCURRENCE, "paid", today.plusDays(1)));
		assertThrows(PaymentConflictException.class,
				() -> move(id, ChangeScope.THIS_AND_FOLLOWING, "paid-batch", today.plusDays(1)));
		assertEquals(0, count("tb_recurrence_command"));
		var future = all().get(1).id();
		move(future, ChangeScope.THIS_OCCURRENCE, "repeat-single", today.plusDays(10));
		move(future, ChangeScope.THIS_OCCURRENCE, "repeat-single", today.plusDays(10));
		assertEquals(1, history.execute(context, historyFilter(future)).content().size());
		assertEquals(today.plusDays(7), history.execute(context, historyFilter(future)).getFirst().serviceDateBefore());
		assertEquals(today.plusDays(10), history.execute(context, historyFilter(future)).getFirst().serviceDateAfter());
	}

	@Test
	void shouldValidateRescheduleHttpScopeAndExposeVersionAndHistory() throws Exception {
		var root = create.execute(context, input(today, null, null));
		var id = all().get(1).id();
		var path = "/api/v1/work-orders/" + id + "/reschedule";
		var body = """
				{"scope":"THIS_AND_FOLLOWING","idempotencyKey":"http-move","serviceDate":"2026-10-12","startTime":"08:30:00"}
				""";
		mvc.perform(post(path).with(user(principal())).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isForbidden());
		mvc.perform(post(path).with(user(principal()))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"serviceDate\":\"2026-10-12\"}")).andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/work-orders/" + id + "/cancel").with(user(principal()))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(
					"{\"scope\":\"THIS_OCCURRENCE\",\"idempotencyKey\":\"null-confirmation\",\"confirmations\":[null]}"))
			.andExpect(status().isBadRequest());
		for (int retry = 0; retry < 2; retry++)
			mvc.perform(post(path).with(user(principal()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)).andExpect(status().isNoContent());
		var next = successor("http-move");
		mvc.perform(get("/api/v1/recurrence-series/" + next).with(user(principal())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lineage.familyId").value(root.getId().toString()))
			.andExpect(jsonPath("$.lineage.previousSeriesId").value(root.getId().toString()))
			.andExpect(jsonPath("$.lineage.firstPosition").value(1));
		mvc.perform(get("/api/v1/work-orders/" + id + "/recurrence-history").with(user(principal())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].reason").value("REPLACED"))
			.andExpect(jsonPath("$.content[0].target.seriesId").value(next.toString()))
			.andExpect(jsonPath("$.content[0].target.occurrenceDate").value("2026-10-12"));
	}

	@Test
	void shouldObserveIndependentReversalCommittedBeforeCollectiveCancellation() throws Exception {
		create.execute(context, input(today, null, null));
		var id = all().getFirst().id();
		var payment = pay.execute(context, new RecordWorkOrderPaymentInput(id, today, actor));
		var locked = new CountDownLatch(1);
		var family = new CountDownLatch(1);
		doAnswer(i -> {
			var result = i.callRealMethod();
			family.countDown();
			return result;
		}).when(org.springframework.test.util.AopTestUtils
			.<RecurrenceRepository>getUltimateTargetObject(seriesRepository)).lockFamily(any(), any());
		try (var pool = Executors.newFixedThreadPool(2)) {
			var financial = pool
				.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactions)
					.execute(tx -> operations.withWorkOrder(account, id, w -> {
						locked.countDown();
						try {
							assertTrue(family.await(10, TimeUnit.SECONDS));
						}
						catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
						return reversePayment.execute(context,
								new ReverseWorkOrderPaymentInput(payment.id(), true, "Independent reversal", actor));
					})));
			assertTrue(locked.await(10, TimeUnit.SECONDS));
			var collective = pool.submit(() -> {
				stop(id, ChangeScope.THIS_AND_FOLLOWING, "after-reversal");
				return true;
			});
			assertNotNull(financial.get(15, TimeUnit.SECONDS));
			assertTrue(collective.get(15, TimeUnit.SECONDS));
		}
		stop(id, ChangeScope.THIS_AND_FOLLOWING, "after-reversal");
		assertTrue(active().isEmpty());
		assertEquals(1, count("tb_customer_payment"));
		assertEquals("Independent reversal",
				paymentRepository.findById(account, payment.id()).orElseThrow().reversal().reason());
	}

	@Test
	void shouldSerializeIsolatedRescheduleWithCollectiveReplacement() throws Exception {
		create.execute(context, input(today, null, null));
		var id = all().getFirst().id();
		try (var pool = Executors.newFixedThreadPool(2)) {
			var start = new CyclicBarrier(2);
			var isolated = pool.submit(() -> {
				start.await();
				try {
					move(id, ChangeScope.THIS_OCCURRENCE, "single-race", today.plusDays(70));
				}
				catch (WorkOrderStateException expected) {
					assertTrue(expected.getMessage().contains("substituída"));
				}
				return true;
			});
			var collective = pool.submit(() -> {
				start.await();
				move(id, ChangeScope.THIS_AND_FOLLOWING, "batch-race", today.plusDays(1));
				return true;
			});
			assertTrue(isolated.get(15, TimeUnit.SECONDS));
			assertTrue(collective.get(15, TimeUnit.SECONDS));
		}
		assertEquals(List.of(today.plusDays(1), today.plusDays(8), today.plusDays(15), today.plusDays(22),
				today.plusDays(29)), active().stream().map(WorkOrder::serviceDate).toList());
		assertEquals(2, count("tb_recurrence_series"));
	}

	@Test
	void shouldNotReopenUngeneratedTailWhenReschedulingBeforeEarlierCollectiveCancellation() {
		var root = create.execute(context, input(today, null, null));
		var old = all();
		// First create a historical empty interval above the terminal cutoff.
		move(old.get(3).id(), ChangeScope.THIS_AND_FOLLOWING, "initial-split", today.plusDays(22));
		stop(old.get(2).id(), ChangeScope.THIS_AND_FOLLOWING, "terminal-cut");
		move(old.get(1).id(), ChangeScope.THIS_AND_FOLLOWING, "earlier-move", today.plusDays(8));
		var next = find.execute(context, successor("earlier-move"));
		assertEquals(2L, next.getLineage().untilPosition());
		assertEquals(List.of(today, today.plusDays(8)), active().stream().map(WorkOrder::serviceDate).toList());
		setTime("2026-12-01T12:00:00Z");
		for (var series : List.of(root.getId(), successor("initial-split"), next.getId()))
			generate.execute(context, series);
		assertEquals(List.of(today, today.plusDays(8)), active().stream().map(WorkOrder::serviceDate).toList());
	}

}
