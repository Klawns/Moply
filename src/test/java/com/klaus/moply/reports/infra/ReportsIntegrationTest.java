package com.klaus.moply.reports.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.factory.PostgresSpringIntegrationTest;
import com.klaus.moply.payments.application.usecase.RecordCollaboratorPayment;
import com.klaus.moply.payments.application.usecase.RecordWorkOrderPayment;
import com.klaus.moply.payments.application.usecase.ReverseCollaboratorPayment;
import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.ports.ReportReadRepository.AssignmentRow;
import com.klaus.moply.reports.application.ports.ReportReadRepository.PaymentRow;
import com.klaus.moply.reports.application.ports.ReportReadRepository.SettlementRow;
import com.klaus.moply.reports.application.ports.ReportReadRepository.WorkRow;
import com.klaus.moply.reports.application.usecase.FindCollaboratorsReport;
import com.klaus.moply.reports.application.usecase.FindCustomerPaymentsReport;
import com.klaus.moply.reports.application.usecase.FindWorkOrdersReport;
import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReportInput;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.reports.application.usecase.dto.WorkOrdersReport;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.pagination.SortQuery;
import com.klaus.moply.shared.application.pagination.SortQuery.Direction;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import com.klaus.moply.workorders.domain.vo.WorkOrderDescription;
import com.klaus.moply.workorders.domain.vo.WorkOrderSchedule;

@SpringBootTest(properties = { "spring.jpa.open-in-view=false", "spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportsIntegrationTest extends PostgresSpringIntegrationTest {

	@Autowired
	WorkOrderRepository works;

	@Autowired
	CustomerRepository customers;

	@Autowired
	CollaboratorRepository collaborators;

	@Autowired
	OrganizationRepository organizations;

	@Autowired
	FindWorkOrdersReport workReport;

	@Autowired
	FindCustomerPaymentsReport paymentsReport;

	@Autowired
	FindCollaboratorsReport collaboratorReport;

	@Autowired
	RecordWorkOrderPayment recordCustomerPayment;

	@Autowired
	RecordCollaboratorPayment recordSettlement;

	@Autowired
	ReverseCollaboratorPayment reverseSettlement;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ReportReadRepository reportReads;

	@Autowired
	MockMvc mvc;

	UUID account;

	UUID customer;

	UUID first;

	UUID second;

	UUID actor = UUID.randomUUID();

	LocalDate today;

	@BeforeEach
	void setup() {
		account = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO tb_organization(id,name,currency_code,timezone,default_work_status) VALUES (?, 'Reports', 'GBP', 'UTC', 'SCHEDULED')",
				account);
		today = LocalDate.now();
		customer = customers.save(account, Customer.create("Customer")).getId();
		first = collaborators.save(account, Collaborator.create(account, "First", null)).getId();
		second = collaborators.save(account, Collaborator.create(account, "Second", null)).getId();
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

	private WorkOrder work(LocalDate date, WorkOrderStatus status) {
		return works.save(account,
				WorkOrder.create(customer, null, new WorkOrderSchedule(date, null), new WorkOrderDescription(null),
						new DurationHours(new BigDecimal("4.00")), new HourlyRate(new BigDecimal("10.00")),
						List.of(first, second), status));
	}

	@Test
	void shouldSeparateRealizedProjectionReceiptDatesAndCollaboratorBalancesWithoutMultiplyingRows() throws Exception {
		var done = work(today.minusDays(5), WorkOrderStatus.COMPLETED);
		work(today.minusDays(5), WorkOrderStatus.COMPLETED);
		var completedFuture = work(today.plusDays(2), WorkOrderStatus.COMPLETED);
		var scheduled = work(today.plusDays(3), WorkOrderStatus.SCHEDULED);
		var context = new Context(account);
		recordCustomerPayment.execute(context, new RecordWorkOrderPayment.Input(done.id(), today, actor));
		var firstSettlement = recordSettlement.execute(context,
				new RecordCollaboratorPayment.Input(done.id(), first, new BigDecimal("5.00"), today, "first", actor));
		recordSettlement.execute(context,
				new RecordCollaboratorPayment.Input(done.id(), first, new BigDecimal("3.00"), today, "second", actor));
		reverseSettlement.execute(context, new ReverseCollaboratorPayment.Input(done.id(), first, firstSettlement.id(),
				true, "Incorrect test entry", actor));
		recordSettlement.execute(context,
				new RecordCollaboratorPayment.Input(done.id(), first, new BigDecimal("2.00"), today, "third", actor));

		var workReportResult = workReport.execute(context,
				new ReportPeriod(today.minusDays(5), today.minusDays(5), null));
		assertEquals(new BigDecimal("80.00"), workReportResult.realizedAmount());
		assertEquals(new BigDecimal("40.00"), workReportResult.realizedPendingAmount());
		assertEquals(new BigDecimal("80.00"), workReportResult.workProjectionAmount());
		assertEquals(2, workReportResult.works().content().size());
		var workPage = workReport.execute(context,
				new ReportPeriod(today.minusDays(5), today.minusDays(5), null, new PageQuery(1, 1, null)));
		assertEquals(1, workPage.works().content().size());
		assertEquals(2, workPage.works().totalElements());
		assertEquals(workReportResult.realizedAmount(), workPage.realizedAmount());

		var futureResult = workReport.execute(context, new ReportPeriod(today.plusDays(2), today.plusDays(3), null));
		assertEquals(BigDecimal.ZERO, futureResult.realizedAmount());
		assertEquals(new BigDecimal("80.00"), futureResult.workProjectionAmount());
		assertTrue(futureResult.works().stream().noneMatch(WorkOrdersReport.Work::realized));

		var receiptsElsewhere = paymentsReport.execute(context, new ReportPeriod(today, today, null));
		assertEquals(new BigDecimal("40.00"), receiptsElsewhere.totalAmount());
		assertEquals(done.id(), receiptsElsewhere.payments().getFirst().workOrderId());
		var receiptsPage = paymentsReport.execute(context,
				new ReportPeriod(today, today, null, new PageQuery(1, 1, null)));
		assertTrue(receiptsPage.payments().isEmpty());
		assertEquals(1, receiptsPage.payments().totalElements());
		assertEquals(receiptsElsewhere.totalAmount(), receiptsPage.totalAmount());
		var noReceiptsOnServiceDate = paymentsReport.execute(context,
				new ReportPeriod(today.minusDays(5), today.minusDays(5), null));
		assertEquals(BigDecimal.ZERO, noReceiptsOnServiceDate.totalAmount());

		var collaboratorsResult = collaboratorReport.execute(context,
				new CollaboratorsReportInput(new ReportPeriod(today.minusDays(5), today.minusDays(5), null), null));
		assertEquals(new BigDecimal("80.00"), collaboratorsResult.allocatedTotal());
		assertEquals(new BigDecimal("80.00"), collaboratorsResult.realizedAllocatedTotal());
		assertEquals(BigDecimal.ZERO, collaboratorsResult.settlementsOnPeriodTotal());
		assertEquals(4, collaboratorsResult.assignments().content().size());
		var assignmentPage = collaboratorReport.execute(context, new CollaboratorsReportInput(
				new ReportPeriod(today.minusDays(5), today.minusDays(5), null, new PageQuery(1, 1, null)), null));
		assertEquals(1, assignmentPage.assignments().content().size());
		assertEquals(4, assignmentPage.assignments().totalElements());
		assertEquals(collaboratorsResult.allocatedTotal(), assignmentPage.allocatedTotal());
		var principal = new AccountPrincipal(
				new AppUser(UUID.randomUUID(), account, new LoginEmail("reports@test"), "unused"));
		mvc.perform(get("/api/v1/reports/work-orders").with(user(principal))
			.param("from", today.minusDays(5).toString())
			.param("to", today.minusDays(5).toString())
			.param("page", "1")
			.param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
				.jsonPath("$.works.content.length()")
				.value(1))
			.andExpect(
					org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.works.totalElements")
						.value(2))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.summary.realizedAmount")
				.value(80.00));
		assertEquals(new BigDecimal("75.00"), collaboratorsResult.realizedPendingTotal());
		var firstAssignment = collaboratorsResult.assignments()
			.stream()
			.filter(a -> a.collaboratorId().equals(first) && a.workOrderId().equals(done.id()))
			.findFirst()
			.orElseThrow();
		assertEquals(new BigDecimal("5.00"), firstAssignment.activeSettlements());
		assertEquals(new BigDecimal("15.00"), firstAssignment.pendingAmount());
		assertTrue(collaboratorsResult.settlements().isEmpty());
		var settlementsElsewhere = collaboratorReport.execute(context,
				new CollaboratorsReportInput(new ReportPeriod(today, today, null), first));
		assertEquals(new BigDecimal("5.00"), settlementsElsewhere.settlementsOnPeriodTotal());
		assertEquals(2, settlementsElsewhere.settlements().content().size());
		assertTrue(futureResult.works().stream().anyMatch(w -> w.workOrderId().equals(scheduled.id())));
		jdbc.update("UPDATE tb_order_service SET status='CANCELLED' WHERE organization_id=? AND id=?", account,
				scheduled.id());
		var afterCancellation = workReport.execute(context,
				new ReportPeriod(today.plusDays(2), today.plusDays(3), null));
		assertEquals(new BigDecimal("40.00"), afterCancellation.workProjectionAmount());
		assertEquals(1, afterCancellation.works().content().size());
	}

	@Test
	void shouldRequireAuthenticationAndValidateInclusivePeriodAndTenantFilters() throws Exception {
		var principal = new AccountPrincipal(
				new AppUser(UUID.randomUUID(), account, new LoginEmail("reports@test"), "unused"));
		mvc.perform(get("/api/v1/reports/work-orders").with(user(principal))).andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/reports/work-orders").param("from", today.toString())
			.param("to", today.minusDays(1).toString())
			.with(user(principal))).andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/reports/work-orders").param("from", today.toString()).param("to", today.toString()))
			.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/reports/customer-payments").param("from", today.toString())
			.param("to", today.toString())
			.param("customerId", UUID.randomUUID().toString())
			.with(user(principal))).andExpect(status().isNotFound());
	}

	@Test
	void shouldReturnEmptyPeriodWithZeroTotals() {
		var context = new Context(account);
		var from = today.minusDays(40);
		var to = today.minusDays(35);
		var workResult = workReport.execute(context, new ReportPeriod(from, to, null));
		var paymentResult = paymentsReport.execute(context, new ReportPeriod(from, to, null));
		var collaboratorResult = collaboratorReport.execute(context,
				new CollaboratorsReportInput(new ReportPeriod(from, to, null), null));
		assertTrue(workResult.works().isEmpty());
		assertEquals(BigDecimal.ZERO, workResult.realizedAmount());
		assertEquals(BigDecimal.ZERO, workResult.realizedPendingAmount());
		assertEquals(BigDecimal.ZERO, workResult.workProjectionAmount());
		assertTrue(paymentResult.payments().isEmpty());
		assertEquals(BigDecimal.ZERO, paymentResult.totalAmount());
		assertTrue(collaboratorResult.assignments().isEmpty());
		assertTrue(collaboratorResult.settlements().isEmpty());
		assertEquals(BigDecimal.ZERO, collaboratorResult.allocatedTotal());
		assertEquals(BigDecimal.ZERO, collaboratorResult.pendingTotal());
	}

	@Test
	void shouldEvaluateReportDateRangePlansOnPostgres() {
		var plans = List.of(
				plan("SELECT id FROM tb_order_service WHERE organization_id='" + account
						+ "' AND service_date BETWEEN DATE '" + today.minusDays(2) + "' AND DATE '" + today
						+ "' AND status <> 'CANCELLED'"),
				plan("SELECT id FROM tb_customer_payment WHERE organization_id='" + account
						+ "' AND status='RECORDED' AND paid_on BETWEEN DATE '" + today.minusDays(2) + "' AND DATE '"
						+ today + "'"),
				plan("SELECT a.work_order_id, a.collaborator_id, sum(a.allocated_amount) FROM tb_work_assignment a "
						+ "JOIN tb_order_service w ON w.organization_id=a.organization_id AND w.id=a.work_order_id "
						+ "WHERE w.organization_id='" + account + "' AND w.service_date BETWEEN DATE '"
						+ today.minusDays(2) + "' AND DATE '" + today
						+ "' AND w.status <> 'CANCELLED' GROUP BY a.work_order_id,a.collaborator_id"),
				plan("SELECT organization_id, work_order_id, collaborator_id, sum(amount) "
						+ "FROM tb_collaborator_payment WHERE status='RECORDED' GROUP BY organization_id,work_order_id,collaborator_id"));
		for (int i = 0; i < plans.size(); i++) {
			assertTrue(plans.get(i).stream().anyMatch(line -> line.contains("Execution Time:")));
			LoggerFactory.getLogger(getClass())
				.info("PostgreSQL report plan {}:\n{}", i + 1, plans.get(i).stream().collect(Collectors.joining("\n")));
		}
	}

	@ParameterizedTest
	@CsvSource({ "serviceDate,ASC", "serviceDate,DESC", "status,ASC", "status,DESC", "id,ASC", "id,DESC" })
	void shouldSortAndPageWorkRows(String field, Direction direction) {
		createPagingData();
		Comparator<WorkRow> order = switch (field) {
			case "serviceDate" -> Comparator.comparing(WorkRow::serviceDate);
			case "status" -> Comparator.comparing(WorkRow::status);
			default -> Comparator.comparing(row -> row.workOrderId().toString());
		};
		assertSortedPages(page -> reportReads.workRows(account, today.minusDays(2), today, null, page), field,
				direction, order, Comparator.comparing(WorkRow::serviceDate), row -> row.workOrderId().toString(), 3);
	}

	@ParameterizedTest
	@CsvSource({ "paidOn,ASC", "paidOn,DESC", "amount,ASC", "amount,DESC", "id,ASC", "id,DESC" })
	void shouldSortAndPageCustomerPayments(String field, Direction direction) {
		createPagingData();
		Comparator<PaymentRow> order = switch (field) {
			case "paidOn" -> Comparator.comparing(PaymentRow::paidOn);
			case "amount" -> Comparator.comparing(PaymentRow::amount);
			default -> Comparator.comparing(row -> row.paymentId().toString());
		};
		assertSortedPages(page -> reportReads.customerPayments(account, today.minusDays(2), today, null, page), field,
				direction, order, Comparator.comparing(PaymentRow::paidOn), row -> row.paymentId().toString(), 3);
	}

	@ParameterizedTest
	@CsvSource({ "collaboratorName,ASC", "collaboratorName,DESC", "serviceDate,ASC", "serviceDate,DESC", "id,ASC",
			"id,DESC" })
	void shouldSortAndPageAssignmentsUsingAssignmentIdForTies(String field, Direction direction) {
		createPagingData();
		Map<String, String> assignmentIds = jdbc
			.query("select id, work_order_id, collaborator_id from tb_work_assignment where organization_id = ?",
					(row, index) -> Map.entry(row.getString("work_order_id") + ":" + row.getString("collaborator_id"),
							row.getString("id")),
					account)
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Function<AssignmentRow, String> id = row -> assignmentIds.get(row.workOrderId() + ":" + row.collaboratorId());
		Comparator<AssignmentRow> order = switch (field) {
			case "collaboratorName" -> Comparator.comparing(AssignmentRow::collaboratorName);
			case "serviceDate" -> Comparator.comparing(AssignmentRow::serviceDate);
			default -> Comparator.comparing(id);
		};
		assertSortedPages(page -> reportReads.assignments(account, today.minusDays(2), today, null, null, page), field,
				direction, order, Comparator.comparing(AssignmentRow::collaboratorName), id, 6);
	}

	@ParameterizedTest
	@CsvSource({ "paidOn,ASC", "paidOn,DESC", "amount,ASC", "amount,DESC", "id,ASC", "id,DESC" })
	void shouldSortAndPageSettlements(String field, Direction direction) {
		createPagingData();
		Comparator<SettlementRow> order = switch (field) {
			case "paidOn" -> Comparator.comparing(SettlementRow::paidOn);
			case "amount" -> Comparator.comparing(SettlementRow::amount);
			default -> Comparator.comparing(row -> row.paymentId().toString());
		};
		assertSortedPages(page -> reportReads.settlements(account, today.minusDays(2), today, null, null, page), field,
				direction, order, Comparator.comparing(SettlementRow::paidOn), row -> row.paymentId().toString(), 3);
	}

	private void createPagingData() {
		var context = new Context(account);
		for (int index = 0; index < 3; index++) {
			var date = index == 0 ? today.minusDays(2) : today.minusDays(1);
			var work = works.save(account,
					WorkOrder.create(customer, null, new WorkOrderSchedule(date, null), new WorkOrderDescription(null),
							new DurationHours(BigDecimal.valueOf(index == 0 ? 2 : 4)), new HourlyRate(BigDecimal.TEN),
							List.of(first, second), WorkOrderStatus.COMPLETED));
			var paidOn = index == 0 ? today.minusDays(1) : today;
			recordCustomerPayment.execute(context, new RecordWorkOrderPayment.Input(work.id(), paidOn, actor));
			recordSettlement.execute(context, new RecordCollaboratorPayment.Input(work.id(), first,
					BigDecimal.valueOf(index == 0 ? 2 : 4), paidOn, "paging-" + index, actor));
			if (index == 0) {
				jdbc.update("update tb_order_service set status = 'SCHEDULED' where organization_id = ? and id = ?",
						account, work.id());
			}
		}
	}

	private <T> void assertSortedPages(Function<PageQuery, PageResult<T>> query, String field, Direction direction,
			Comparator<T> primaryOrder, Comparator<T> defaultOrder, Function<T, String> id, int expectedCount) {
		var baseline = query.apply(PageQuery.defaults());
		assertEquals(expectedCount, baseline.totalElements());
		assertEquals(expectedCount, baseline.content().size());
		assertEquals(baseline.content().stream().sorted(defaultOrder.thenComparing(id)).toList(), baseline.content());

		var order = direction == Direction.ASC ? primaryOrder : primaryOrder.reversed();
		if (!field.equals("id")) {
			order = order.thenComparing(id);
		}
		var expected = baseline.content().stream().sorted(order).toList();
		for (int size : List.of(2, 4)) {
			int totalPages = (expectedCount + size - 1) / size;
			for (int page = 0; page <= totalPages; page++) {
				var result = query.apply(new PageQuery(page, size, new SortQuery(field, direction)));
				int start = Math.min(page * size, expectedCount);
				int end = Math.min(start + size, expectedCount);
				assertEquals(expected.subList(start, end), result.content());
				assertEquals(expectedCount, result.totalElements());
				assertEquals(totalPages, result.totalPages());
				assertEquals(page, result.page());
			}
		}
		assertThrows(DomainException.class,
				() -> query.apply(new PageQuery(0, 2, new SortQuery("unknown", Direction.ASC))));
	}

	@Test
	void shouldApplyCustomerCollaboratorAndOrganizationFiltersToRowsAndTotals() {
		createPagingData();
		var page = PageQuery.defaults();
		var from = today.minusDays(2);
		var filteredAssignments = reportReads.assignments(account, from, today, customer, first, page);
		assertEquals(3, filteredAssignments.totalElements());
		assertTrue(filteredAssignments.stream()
			.allMatch(row -> row.customerId().equals(customer) && row.collaboratorId().equals(first)));
		assertEquals(3, reportReads.workRows(account, from, today, customer, page).totalElements());
		assertEquals(3, reportReads.customerPayments(account, from, today, customer, page).totalElements());
		assertEquals(3, reportReads.settlements(account, from, today, customer, first, page).totalElements());
		assertEquals(0, reportReads.settlements(account, from, today, customer, second, page).totalElements());
		assertEquals(new BigDecimal("50.00"),
				reportReads.collaboratorTotals(account, from, today, customer, second, today).allocated());
		assertEquals(BigDecimal.ZERO,
				reportReads.collaboratorTotals(account, from, today, customer, second, today).settlements());

		var otherCustomer = customers.save(account, Customer.create("Other customer")).getId();
		assertEmptyReports(account, otherCustomer, null);
		assertEmptyReports(UUID.randomUUID(), null, null);
		assertEquals(0,
				reportReads.assignments(account, from, today, customer, UUID.randomUUID(), page).totalElements());
	}

	private void assertEmptyReports(UUID organizationId, UUID customerId, UUID collaboratorId) {
		var page = PageQuery.defaults();
		var from = today.minusDays(2);
		var results = List.of(reportReads.workRows(organizationId, from, today, customerId, page),
				reportReads.customerPayments(organizationId, from, today, customerId, page),
				reportReads.assignments(organizationId, from, today, customerId, collaboratorId, page),
				reportReads.settlements(organizationId, from, today, customerId, collaboratorId, page));
		for (var result : results) {
			assertTrue(result.isEmpty());
			assertEquals(0, result.totalElements());
			assertEquals(0, result.totalPages());
		}
		assertEquals(new ReportReadRepository.WorkTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
				reportReads.workTotals(organizationId, from, today, customerId, today));
		assertEquals(BigDecimal.ZERO, reportReads.customerPaymentTotal(organizationId, from, today, customerId));
		assertEquals(
				new ReportReadRepository.CollaboratorTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
				reportReads.collaboratorTotals(organizationId, from, today, customerId, collaboratorId, today));
	}

	private List<String> plan(String sql) {
		return jdbc.queryForList("EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) " + sql, String.class);
	}

}
