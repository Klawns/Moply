package com.klaus.moply.shared.infra.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.klaus.moply.reports.application.usecase.FindWorkOrdersReport;
import com.klaus.moply.reports.application.usecase.FindCustomerPaymentsReport;
import com.klaus.moply.reports.application.usecase.FindCollaboratorsReport;
import com.klaus.moply.reports.application.usecase.dto.WorkOrdersReport;
import com.klaus.moply.reports.application.usecase.dto.CustomerPaymentsReport;
import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReport;
import com.klaus.moply.shared.application.pagination.PageResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.domain.entities.Customer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebDtoContractIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	CustomerRepository customers;

	@Autowired
	CollaboratorRepository collaborators;

	@MockitoBean
	FindWorkOrdersReport workOrdersReport;

	@MockitoBean
	FindCustomerPaymentsReport customerPaymentsReport;

	@MockitoBean
	FindCollaboratorsReport collaboratorsReport;

	UUID organizationId, customerId, first, second, actor;

	AccountPrincipal principal;

	LocalDate today;

	@BeforeEach
	void setup() {
		organizationId = UUID.randomUUID();
		actor = UUID.randomUUID();
		today = LocalDate.now(ZoneOffset.UTC);
		jdbc.update(
				"INSERT INTO tb_organization(id,name,currency_code,timezone,default_work_status,default_hourly_rate) VALUES (?,'DTO contract','GBP','UTC','SCHEDULED',30)",
				organizationId);
		customerId = customers.save(organizationId, Customer.create("Client")).getId();
		first = collaborators.save(organizationId, Collaborator.create(organizationId, "First", null)).getId();
		second = collaborators.save(organizationId, Collaborator.create(organizationId, "Second", null)).getId();
		principal = new AccountPrincipal(
				new AppUser(actor, organizationId, new LoginEmail("dto@example.com"), "unused"));
	}

	String conditions() {
		return """
				{"customerId":"%s","contractedHours":4,"participantIds":["%s","%s"]}
				""".formatted(customerId, second, first).strip();
	}

	String workRequest() {
		return "{\"serviceDate\":\"" + today + "\",\"conditions\":" + conditions() + "}";
	}

	ResultActions write(String path, String body) throws Exception {
		return mvc.perform(
				post(path).with(user(principal)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	String id(ResultActions response) throws Exception {
		return JsonPath.read(response.andReturn().getResponse().getContentAsString(), "$.id");
	}

	@Test
	void shouldUseTheSameWorkContractForCreationDetailAndPagination() throws Exception {
		var created = write("/api/v1/work-orders", workRequest()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.customer.id").value(customerId.toString()))
			.andExpect(jsonPath("$.customer.name").value("Client"))
			.andExpect(jsonPath("$.schedule.serviceDate").value(today.toString()))
			.andExpect(jsonPath("$.pricing.hourlyRate").value(30))
			.andExpect(jsonPath("$.pricing.totalAmount").value(120))
			.andExpect(jsonPath("$.status").value("SCHEDULED"))
			.andExpect(jsonPath("$.assignments[0].collaboratorId").value(second.toString()))
			.andExpect(jsonPath("$.assignments[1].collaboratorId").value(first.toString()))
			.andExpect(jsonPath("$.customerId").doesNotExist())
			.andExpect(jsonPath("$.totalAmount").doesNotExist())
			.andExpect(jsonPath("$.recurrence").value(org.hamcrest.Matchers.nullValue()));
		String workId = id(created);
		String detail = mvc.perform(get("/api/v1/work-orders/" + workId).with(user(principal)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat((Object) JsonPath.read(detail, "$"))
			.isEqualTo(JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$"));
		mvc.perform(get("/api/v1/work-orders").param("customerId", customerId.toString()).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(workId))
			.andExpect(jsonPath("$.content[0].pricing.totalAmount").value(120))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void shouldReturnTheSamePreviewInPricingErrorsAndPreserveAcceptance() throws Exception {
		collaborators.save(organizationId,
				collaborators.findById(organizationId, second)
					.orElseThrow()
					.update("Second", null, new BigDecimal("20")));
		String preview = write("/api/v1/work-orders/pricing-preview", workRequest()).andExpect(status().isOk())
			.andExpect(jsonPath("$.pricing.totalAmount").value(120))
			.andExpect(jsonPath("$.summary.baseTotal").value(100))
			.andExpect(jsonPath("$.summary.surplusAmount").value(20))
			.andExpect(jsonPath("$.participants[0].collaboratorId").value(second.toString()))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String problem = write("/api/v1/work-orders", workRequest()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PRICING_ACCEPTANCE_REQUIRED"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat((Object) JsonPath.read(problem, "$.pricingPreview")).isEqualTo(JsonPath.read(preview, "$"));
		String fingerprint = JsonPath.read(preview, "$.pricingFingerprint");
		String accepted = workRequest().substring(0, workRequest().length() - 1) + ",\"acceptedPricingFingerprint\":\""
				+ fingerprint + "\"}";
		write("/api/v1/work-orders", accepted).andExpect(status().isCreated())
			.andExpect(jsonPath("$.assignments[0].allocatedAmount").value(50));
		collaborators.save(organizationId,
				collaborators.findById(organizationId, second)
					.orElseThrow()
					.update("Second", null, new BigDecimal("100")));
		write("/api/v1/work-orders", workRequest()).andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.pricingPreview.summary.excessAmount").value(140));
	}

	@Test
	void shouldRejectMissingNestedObjectsAndInvalidConditions() throws Exception {
		for (String invalid : new String[] { "{}", "{\"serviceDate\":\"" + today + "\",\"conditions\":null}",
				workRequest().replace("\"customerId\":\"" + customerId + "\"", "\"customerId\":null"),
				workRequest().replace(second.toString(), "invalid-uuid"), workRequest().replace(":4,", ":0,") }) {
			write("/api/v1/work-orders", invalid).andExpect(status().isBadRequest());
		}
		write("/api/v1/recurrence-series", "{\"frequency\":\"WEEKLY\",\"conditions\":" + conditions() + "}")
			.andExpect(status().isBadRequest());
		write("/api/v1/recurrence-series",
				"{\"frequency\":\"WEEKLY\",\"period\":{},\"conditions\":" + conditions() + "}")
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldGroupSeriesPricingLineageAndOccurrenceHistory() throws Exception {
		String body = "{\"frequency\":\"WEEKLY\",\"period\":{\"startsOn\":\"" + today + "\",\"endsOn\":\"" + today
				+ "\"},\"conditions\":" + conditions() + "}";
		var created = write("/api/v1/recurrence-series", body).andExpect(status().isCreated())
			.andExpect(jsonPath("$.period.startsOn").value(today.toString()))
			.andExpect(jsonPath("$.conditions.pricing.hourlyRate").value(30))
			.andExpect(jsonPath("$.conditions.frozenPricing.assignments[0].collaboratorId").value(second.toString()))
			.andExpect(jsonPath("$.lineage.firstPosition").value(0))
			.andExpect(jsonPath("$.familyId").doesNotExist());
		String seriesId = id(created);
		mvc.perform(get("/api/v1/recurrence-series/" + seriesId).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lineage.familyId").value(seriesId));
		String works = mvc
			.perform(get("/api/v1/work-orders").param("customerId", customerId.toString()).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].recurrence.seriesId").value(seriesId))
			.andExpect(jsonPath("$.content[0].recurrence.occurrenceDate").value(today.toString()))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String workId = JsonPath.read(works, "$.content[0].id");
		write("/api/v1/work-orders/" + workId + "/reschedule",
				"{\"serviceDate\":\"" + today.plusDays(1)
						+ "\",\"scope\":\"THIS_OCCURRENCE\",\"idempotencyKey\":\"dto-reschedule\"}")
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/work-orders/" + workId + "/recurrence-history").with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].target.position").value(0))
			.andExpect(jsonPath("$.content[0].serviceDateChange.before").value(today.toString()))
			.andExpect(jsonPath("$.content[0].serviceDateChange.after").value(today.plusDays(1).toString()));
	}

	@Test
	void shouldGroupPaymentAuditAndBalances() throws Exception {
		String workId = id(write("/api/v1/work-orders", workRequest()).andExpect(status().isCreated()));
		write("/api/v1/work-orders/" + workId + "/complete", "").andExpect(status().isNoContent());
		String payments = "/api/v1/work-orders/" + workId + "/payments";
		String paymentId = id(write(payments, "{\"paidOn\":\"" + today + "\"}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.recording.by").value(actor.toString()))
			.andExpect(jsonPath("$.recording.at").isNotEmpty())
			.andExpect(jsonPath("$.reversal").value(org.hamcrest.Matchers.nullValue())));
		mvc.perform(get(payments).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].recording.by").value(actor.toString()));
		write("/api/v1/payments/" + paymentId + "/reversal", "{\"confirmNoMoneyReceived\":true,\"reason\":\"Mistake\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reversal.by").value(actor.toString()))
			.andExpect(jsonPath("$.reversal.reason").value("Mistake"));
		mvc.perform(get("/api/v1/collaborators/" + second + "/payments/summary").with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance.allocatedAmount").value(60))
			.andExpect(jsonPath("$.balance.recordedAmount").value(0))
			.andExpect(jsonPath("$.workOrders[0].balance.remainingAmount").value(60));
	}

	@Test
	void shouldGroupReportTotalsAndResourceReferences() throws Exception {
		UUID workId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		BigDecimal total = new BigDecimal("120");
		BigDecimal allocated = new BigDecimal("60");
		BigDecimal paid = new BigDecimal("20");
		BigDecimal zero = BigDecimal.ZERO;
		when(workOrdersReport.execute(any(), any())).thenReturn(new WorkOrdersReport(today, today, today, "UTC", "GBP",
				total, zero, zero, new PageResult<>(List.of(new WorkOrdersReport.Work(workId, customerId, "Client",
						today, "COMPLETED", total, true, true, false)), 0, 20, 1, 1)));
		when(customerPaymentsReport.execute(any(), any()))
			.thenReturn(new CustomerPaymentsReport(today, today, "UTC", "GBP", total,
					new PageResult<>(List
						.of(new CustomerPaymentsReport.Payment(paymentId, workId, customerId, "Client", today, total)),
							0, 20, 1, 1)));
		when(collaboratorsReport.execute(any(), any())).thenReturn(new CollaboratorsReport(today, today, today, "UTC",
				"GBP", allocated, allocated, zero, allocated.subtract(paid), allocated.subtract(paid), zero, paid,
				new PageResult<>(List.of(new CollaboratorsReport.Assignment(workId, customerId, "Client", today,
						"COMPLETED", second, "Second", allocated, paid, allocated.subtract(paid), true)), 0, 20, 1, 1),
				new PageResult<>(List.of(new CollaboratorsReport.Settlement(paymentId, workId, customerId, "Client",
						today, second, "Second", today, paid)), 0, 20, 1, 1)));
		String filters = "?from=" + today + "&to=" + today + "&customerId=" + customerId;
		mvc.perform(get("/api/v1/reports/work-orders" + filters).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.period.from").value(today.toString()))
			.andExpect(jsonPath("$.context.currencyCode").value("GBP"))
			.andExpect(jsonPath("$.summary.realizedAmount").value(120))
			.andExpect(jsonPath("$.works.content[0].customer.id").value(customerId.toString()))
			.andExpect(jsonPath("$.works.content[0].customer.name").value("Client"))
			.andExpect(jsonPath("$.realizedAmount").doesNotExist());
		mvc.perform(get("/api/v1/reports/customer-payments" + filters).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.summary.totalAmount").value(120))
			.andExpect(jsonPath("$.context.referenceDate").value(org.hamcrest.Matchers.nullValue()))
			.andExpect(jsonPath("$.payments.content[0].customer.id").value(customerId.toString()));
		mvc.perform(get("/api/v1/reports/collaborators" + filters + "&collaboratorId=" + second).with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.summary.allocatedTotal").value(60))
			.andExpect(jsonPath("$.summary.settlementsOnPeriodTotal").value(20))
			.andExpect(jsonPath("$.assignments.content[0].collaborator.id").value(second.toString()))
			.andExpect(jsonPath("$.settlements.content[0].customer.name").value("Client"));
		verify(workOrdersReport).execute(argThat(context -> context.organizationId().equals(organizationId)),
				argThat(period -> period.from().equals(today) && period.to().equals(today)
						&& period.customerId().equals(customerId)));
		verify(customerPaymentsReport).execute(argThat(context -> context.organizationId().equals(organizationId)),
				argThat(period -> period.customerId().equals(customerId)));
		verify(collaboratorsReport)
			.execute(argThat(context -> context.organizationId().equals(organizationId)), argThat(
					input -> input.period().customerId().equals(customerId) && input.collaboratorId().equals(second)));

	}

}
