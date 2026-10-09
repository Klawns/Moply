package com.klaus.moply.workorders.infra.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.klaus.moply.workorders.application.service.WorkOrderPreparation;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.klaus.moply.payments.application.usecase.dto.RecordCollaboratorPaymentInput;
import com.jayway.jsonpath.JsonPath;
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
import com.klaus.moply.reports.application.usecase.*;
import com.klaus.moply.reports.application.usecase.dto.*;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;

@SpringBootTest(properties = { "spring.jpa.open-in-view=false", "spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HourlyRateApiIntegrationTest extends PostgresSpringIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	CustomerRepository customers;

	@Autowired
	CollaboratorRepository people;

	@Autowired
	OrganizationRepository organizations;

	@MockitoSpyBean
	WorkOrderPreparation preparation;

	@Autowired
	WorkOrderRepository orders;

	@Autowired
	RecordCollaboratorPayment settle;

	@Autowired
	GetCollaboratorPaymentSummary summary;

	@Autowired
	FindCollaboratorsReport collaboratorReport;

	@Autowired
	FindWorkOrdersReport workReport;

	UUID org, foreignOrg, customer, ana, bruno, actor;

	AccountPrincipal principal;

	@BeforeEach
	void setup() {
		org = organization();
		foreignOrg = organization();
		actor = UUID.randomUUID();
		customer = customers.save(org, Customer.create("Client")).getId();
		ana = people.save(org, Collaborator.create(org, "Ana", null, new BigDecimal("20"))).getId();
		bruno = people.save(org, Collaborator.create(org, "Bruno", null)).getId();
		organizations.updatePreferences(org,
				organization -> organization.withPreferences("UTC", DefaultWorkStatus.SCHEDULED, new BigDecimal("30")));
		principal = principal(org);
	}

	UUID organization() {
		var id = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO tb_organization(id,name,currency_code,timezone,default_work_status) VALUES (?,'Test','GBP','UTC','SCHEDULED')",
				id);
		return id;
	}

	AccountPrincipal principal(UUID id) {
		return new AccountPrincipal(new AppUser(actor, id, new LoginEmail("rate@example.com"), "unused"));
	}

	MockHttpServletRequestBuilder request(String path, String body) {
		return post(path).with(user(principal)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	String input(String extra) {
		// Keep acceptance at the request root and work conditions in their own object.
		String acceptance = "";
		int marker = extra.indexOf(",\"acceptedPricingFingerprint\"");
		if (marker >= 0) {
			acceptance = extra.substring(marker);
			extra = extra.substring(0, marker);
		}
		return "{\"serviceDate\":\"" + LocalDate.now(java.time.ZoneOffset.UTC)
				+ "\",\"conditions\":{\"customerId\":\"" + customer
				+ "\",\"contractedHours\":4,\"participantIds\":[\"" + ana + "\",\"" + bruno + "\"]"
				+ extra + "}" + acceptance + "}";
	}

	String preview(String extra) throws Exception {
		return mvc.perform(request("/api/v1/work-orders/pricing-preview", input(extra)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	String fingerprint(String preview) {
		return JsonPath.read(preview, "$.pricingFingerprint");
	}

	@AfterEach
	void cleanup() {
		for (String table : List.of("tb_recurrence_change_item", "tb_recurrence_command", "tb_recurrence_exclusion",
				"tb_collaborator_payment", "tb_customer_payment", "tb_work_assignment", "tb_order_service",
				"tb_recurrence_member", "tb_recurrence_series", "tb_customer_location", "tb_customer",
				"tb_collaborator", "tb_app_user"))
			jdbc.update("DELETE FROM " + table + " WHERE organization_id IN (?,?)", org, foreignOrg);
		jdbc.update("DELETE FROM tb_organization WHERE id IN (?,?)", org, foreignOrg);
	}

	@Test
	void shouldPreviewRequireConfirmationAndPersistAcceptedFinancialBreakdown() throws Exception {
		doAnswer(invocation -> {
			assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
			assertEquals(java.sql.Connection.TRANSACTION_REPEATABLE_READ,
					TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
			return invocation.callRealMethod();
		}).when(preparation).preview(any(), any());
		doAnswer(invocation -> {
			assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
			assertEquals(java.sql.Connection.TRANSACTION_REPEATABLE_READ,
					TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
			return invocation.callRealMethod();
		}).when(preparation).prepare(any(), any());
		var result = mvc.perform(request("/api/v1/work-orders/pricing-preview", input("")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requiresConfirmation").value(true))
			.andExpect(jsonPath("$.participants[0].baseAmount").value(40))
			.andExpect(jsonPath("$.participants[0].surplusAmount").value(10))
			.andExpect(jsonPath("$.participants[0].rateSource").value("COLLABORATOR"))
			.andExpect(jsonPath("$.participants[1].allocatedAmount").value(70))
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertEquals(0,
				orders.findAll(org, new com.klaus.moply.workorders.domain.vo.WorkOrderDateRange(null, null), null)
					.size());
		mvc.perform(request("/api/v1/work-orders", input("")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PRICING_ACCEPTANCE_REQUIRED"))
			.andExpect(jsonPath("$.pricingPreview.participants[0].allocatedAmount").value(50));
		var saved = mvc
			.perform(request("/api/v1/work-orders",
					input(",\"acceptedPricingFingerprint\":\"" + fingerprint(result) + "\"")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.assignments[0].allocatedAmount").value(50))
			.andExpect(jsonPath("$.assignments[0].appliedHourlyRate").value(20))
			.andExpect(jsonPath("$.pricing.allocationPolicyVersion").value(2))
			.andReturn()
			.getResponse()
			.getContentAsString();
		UUID id = UUID.fromString(JsonPath.read(saved, "$.id"));
		people.save(org, people.findById(org, ana).orElseThrow().update("Ana", null, new BigDecimal("40")));
		organizations.updatePreferences(org,
				organization -> organization.withPreferences("UTC", DefaultWorkStatus.COMPLETED, new BigDecimal("50")));
		mvc.perform(get("/api/v1/work-orders/" + id).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pricing.hourlyRate").value(30))
			.andExpect(jsonPath("$.assignments[0].allocatedAmount").value(50));
		var work = orders.findById(org, id).orElseThrow();
		assertEquals(new BigDecimal("10.00"), work.assignments().getFirst().surplusAmount().value());
	}

	@Test
	void shouldRejectStaleAndUnconfirmablePreviewsWithoutWrites() throws Exception {
		var accepted = fingerprint(preview(""));
		people.save(org, people.findById(org, ana).orElseThrow().update("Ana", null, new BigDecimal("25")));
		mvc.perform(request("/api/v1/work-orders", input(",\"acceptedPricingFingerprint\":\"" + accepted + "\"")))
			.andExpect(status().isConflict());
		mvc.perform(request("/api/v1/work-orders",
				input(",\"hourlyRate\":10,\"acceptedPricingFingerprint\":\""
						+ fingerprint(preview(",\"hourlyRate\":10")) + "\"")))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("PRICING_BASES_EXCEED_TOTAL"))
			.andExpect(jsonPath("$.pricingPreview.summary.excessAmount").value(30));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_order_service WHERE organization_id=?",
				Integer.class, org));
	}

	@Test
	void shouldPreserveOmittedPreferenceAndClearExplicitNull() throws Exception {
		String path = "/api/v1/accounts/me/preferences";
		mvc.perform(get(path).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.defaultHourlyRate").value(30));
		mvc.perform(put(path).with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"timezone\":\"UTC\",\"defaultWorkStatus\":\"SCHEDULED\"}")).andExpect(status().isNoContent());
		assertEquals(new BigDecimal("30.00"), organizations.findById(org).orElseThrow().defaultHourlyRate());
		for (String invalid : List.of("0", "-1", "1.001"))
			mvc.perform(put(path).with(user(principal))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"timezone\":\"UTC\",\"defaultWorkStatus\":\"SCHEDULED\",\"defaultHourlyRate\":" + invalid
						+ "}"))
				.andExpect(status().isBadRequest());
		mvc.perform(put(path).with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"timezone\":\"UTC\",\"defaultWorkStatus\":\"SCHEDULED\",\"defaultHourlyRate\":null}"))
			.andExpect(status().isNoContent());
		assertNull(organizations.findById(org).orElseThrow().defaultHourlyRate());
		mvc.perform(request("/api/v1/work-orders/pricing-preview", input(""))).andExpect(status().isBadRequest());
		mvc.perform(request("/api/v1/work-orders/pricing-preview", input(",\"hourlyRate\":30")))
			.andExpect(status().isOk());
		assertNull(organizations.findById(foreignOrg).orElseThrow().defaultHourlyRate());
	}

	@Test
	void shouldUpdateDefaultRateThroughApiAndUseExplicitNullAsFallback() throws Exception {
		mvc.perform(put("/api/v1/accounts/me/preferences").with(user(principal))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"timezone\":\"UTC\",\"defaultWorkStatus\":\"SCHEDULED\",\"defaultHourlyRate\":35}"))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/accounts/me/preferences").with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.defaultHourlyRate").value(35));
		mvc.perform(get("/api/v1/accounts/me/preferences").with(user(principal(foreignOrg))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.defaultHourlyRate").doesNotExist());
		mvc.perform(request("/api/v1/work-orders/pricing-preview", input(",\"hourlyRate\":null")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pricing.hourlyRate").value(35));
		mvc.perform(request("/api/v1/work-orders/pricing-preview", input(",\"hourlyRate\":30")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pricing.hourlyRate").value(30));
	}

	@Test
	void shouldConfirmSeriesCreationAndExposeFrozenPricingInItsResponse() throws Exception {
		String seriesBody = input("").replace("\"serviceDate\":\"" + LocalDate.now(java.time.ZoneOffset.UTC) + "\"",
				"\"period\":{\"startsOn\":\"" + LocalDate.now(java.time.ZoneOffset.UTC) + "\"}");
		seriesBody = seriesBody.substring(0, seriesBody.length() - 1) + ",\"frequency\":\"WEEKLY\"}";
		mvc.perform(request("/api/v1/recurrence-series", seriesBody))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.pricingPreview.participants[0].allocatedAmount").value(50));
		var fingerprint = fingerprint(preview(""));
		seriesBody = seriesBody.substring(0, seriesBody.length() - 1) + ",\"acceptedPricingFingerprint\":\""
				+ fingerprint + "\"}";
		var saved = mvc.perform(request("/api/v1/recurrence-series", seriesBody))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.conditions.frozenPricing.totalAmount").value(120))
			.andExpect(jsonPath("$.conditions.frozenPricing.assignments[0].allocatedAmount").value(50))
			.andReturn()
			.getResponse()
			.getContentAsString();
		UUID id = UUID.fromString(JsonPath.read(saved, "$.id"));
		mvc.perform(get("/api/v1/recurrence-series/" + id).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.conditions.frozenPricing.assignments[1].allocatedAmount").value(70));
	}

	@Test
	void shouldRejectForeignParticipantsAndProtectPreviewWithAuthenticationAndCsrf() throws Exception {
		var foreign = people.save(foreignOrg, Collaborator.create(foreignOrg, "Foreign", null, new BigDecimal("5")))
			.getId();
		mvc.perform(
				request("/api/v1/work-orders/pricing-preview", input("").replace(ana.toString(), foreign.toString())))
			.andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/work-orders/pricing-preview").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(input(""))).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/work-orders/pricing-preview").with(user(principal))
			.contentType(MediaType.APPLICATION_JSON)
			.content(input(""))).andExpect(status().isForbidden());
	}

	@Test
	void shouldKeepLegacyCreationWithoutConfirmationWhenRatesMatch() throws Exception {
		people.save(org, people.findById(org, ana).orElseThrow().update("Ana", null, new BigDecimal("30")));
		mvc.perform(request("/api/v1/work-orders", input("")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.pricing.allocationPolicyVersion").value(1))
			.andExpect(jsonPath("$.assignments[0].allocatedAmount").value(60))
			.andExpect(jsonPath("$.assignments[1].allocatedAmount").value(60));
	}

	@Test
	void shouldUseApprovedAllocationsInPaymentsBalancesAndReports() throws Exception {
		var saved = mvc
			.perform(request("/api/v1/work-orders",
					input(",\"initialStatus\":\"COMPLETED\",\"acceptedPricingFingerprint\":\""
							+ fingerprint(preview(",\"initialStatus\":\"COMPLETED\"")) + "\"")))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		UUID id = UUID.fromString(JsonPath.read(saved, "$.id"));
		var context = new Context(org);
		var today = LocalDate.now(java.time.ZoneOffset.UTC);
		settle.execute(context,
				new RecordCollaboratorPaymentInput(id, ana, new BigDecimal("20"), today, "partial", actor));
		assertThrows(PaymentConflictException.class, () -> settle.execute(context,
				new RecordCollaboratorPaymentInput(id, ana, new BigDecimal("31"), today, "exceeds", actor)));
		var balance = summary.execute(context, ana);
		assertEquals(new BigDecimal("50.00"), balance.allocatedAmount());
		assertEquals(new BigDecimal("30.00"), balance.remainingAmount());
		var period = new ReportPeriod(today, today, null);
		var report = collaboratorReport.execute(context, new CollaboratorsReportInput(period, ana));
		assertEquals(new BigDecimal("50.00"), report.allocatedTotal());
		assertEquals(new BigDecimal("30.00"), report.pendingTotal());
		var clientReport = workReport.execute(context, period);
		assertEquals(new BigDecimal("120.00"), clientReport.realizedAmount());
		settle.execute(context,
				new RecordCollaboratorPaymentInput(id, ana, new BigDecimal("30"), today, "remaining", actor));
		assertEquals(new BigDecimal("0.00"), summary.execute(context, ana).remainingAmount());
	}

}
