package com.klaus.moply.shared.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.jayway.jsonpath.JsonPath;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = { "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	RequestMappingHandlerMapping mappings;

	@Test
	void shouldDocumentEveryControllerOperationFromItsInterface() throws Exception {
		Map<String, Object> document = document();
		Map<String, Map<String, Map<String, Object>>> paths = value(document, "paths");
		Set<Class<?>> controllers = new HashSet<>();
		Set<String> documentedOperations = new HashSet<>();
		mappings.getHandlerMethods().forEach((mapping, handler) -> {
			if (!handler.getBeanType().getPackageName().startsWith("com.klaus.moply")) {
				return;
			}
			controllers.add(handler.getBeanType());
			assertNoSwaggerAnnotations(handler.getBeanType());
			assertNoSwaggerAnnotations(handler.getMethod());
			Arrays.stream(handler.getMethod().getParameters()).forEach(this::assertNoSwaggerAnnotations);
			assertThat(handler.getBeanType().getInterfaces()).hasSize(1);
			assertThat(handler.getBeanType().getInterfaces()[0].getAnnotation(Tag.class)).isNotNull();
			mapping.getPatternValues().forEach(path -> mapping.getMethodsCondition().getMethods().forEach(method -> {
				String verb = method.name().toLowerCase();
				assertThat(paths).containsKey(path);
				assertThat(paths.get(path)).containsKey(verb);
				Map<String, Object> operation = paths.get(path).get(verb);
				assertThat((String) operation.get("summary")).isNotBlank();
				assertThat((List<String>) operation.get("tags")).hasSize(1);
				Map<String, Object> responses = value(operation, "responses");
				assertThat(responses.keySet()).anyMatch(code -> code.startsWith("2"));
				List<Map<String, Object>> parameters = value(operation, "parameters");
				if (parameters != null) {
					assertThat(parameters).noneMatch(
							parameter -> Set.of("principal", "token", "request").contains(parameter.get("name")));
				}
				documentedOperations.add(path + ":" + verb);
			}));
		});
		assertThat(controllers).hasSize(16);
		Set<String> actualOperations = new HashSet<>();
		paths.forEach(
				(path, operations) -> operations.keySet().forEach(verb -> actualOperations.add(path + ":" + verb)));
		documentedOperations.add("/api/v1/auth/login:post");
		documentedOperations.add("/api/v1/auth/logout:post");
		assertThat(actualOperations).isEqualTo(documentedOperations);
		Map<String, Object> components = value(document, "components");
		Map<String, Object> schemas = value(components, "schemas");
		assertThat(schemas).containsKeys("ProblemDetail", "RegistrationRequest", "PreferencesRequest", "CsrfResponse");
		assertSchemaReferencesResolve(document, schemas);
	}

	@Test
	void shouldDescribeBodiesPaginationErrorsAndAuthentication() throws Exception {
		Map<String, Object> document = document();
		Map<String, Map<String, Map<String, Object>>> paths = value(document, "paths");
		Map<String, Object> create = paths.get("/api/v1/work-orders").get("post");
		assertThat(value(create, "responses", Map.class)).containsKeys("201", "400", "401", "403", "404", "409", "422");
		Map<String, Object> cancel = paths.get("/api/v1/work-orders/{id}/cancel").get("post");
		Map<String, Object> body = value(cancel, "requestBody");
		assertThat(body.get("required")).isNotEqualTo(true);
		Map<String, Map<String, Object>> responses = value(cancel, "responses");
		assertThat(responses.get("204")).doesNotContainKey("content");

		List<Map<String, Object>> parameters = value(paths.get("/api/v1/customers").get("get"), "parameters");
		assertThat(parameter(parameters, "page")).containsEntry("default", 0).containsEntry("minimum", 0);
		assertThat(parameter(parameters, "size")).containsEntry("default", 20).containsEntry("maximum", 100);
		assertThat(parameter(parameters, "sort")).containsEntry("enum", List.of("name", "id"));
		assertThat(parameter(parameters, "direction")).doesNotContainKey("default");
		for (String report : List.of("work-orders", "customer-payments", "collaborators")) {
			List<Map<String, Object>> filters = value(paths.get("/api/v1/reports/" + report).get("get"), "parameters");
			assertThat(filters.stream().map(filter -> filter.get("name"))).doesNotHaveDuplicates()
				.contains("from", "to", "customerId", "page", "size", "sort", "direction");
			assertThat(filters).filteredOn(filter -> List.of("from", "to").contains(filter.get("name")))
				.allMatch(filter -> Boolean.TRUE.equals(filter.get("required")));
		}
		Map<String, Object> components = value(document, "components");
		Map<String, Map<String, Object>> schemes = value(components, "securitySchemes");
		assertThat(schemes.get("cookieAuth")).containsEntry("in", "cookie").containsEntry("name", "MOPLY_AUTH");
		assertThat(schemes.get("csrfToken")).containsEntry("in", "header").containsEntry("name", "X-XSRF-TOKEN");
		Map<String, Object> login = paths.get("/api/v1/auth/login").get("post");
		Map<String, Object> loginBody = value(login, "requestBody");
		Map<String, Map<String, Object>> content = value(loginBody, "content");
		Map<String, Object> credentials = value(content.get("application/x-www-form-urlencoded"), "schema");
		assertThat(credentials.get("required")).isEqualTo(List.of("email", "password"));
		assertThat(value(login, "responses", Map.class)).containsKeys("204", "401", "403");
		assertThat(value(paths.get("/api/v1/auth/logout").get("post"), "responses", Map.class)).containsKeys("204",
				"403");
		assertThat(value(paths.get("/api/v1/accounts").get("post"), "security", List.class))
			.containsExactly(Map.of("csrfToken", List.of()));
		assertThat(value(create, "security", List.class))
			.containsExactly(Map.of("cookieAuth", List.of(), "csrfToken", List.of()));
		assertThat(value(paths.get("/api/v1/auth/csrf").get("get"), "security", List.class)).isNullOrEmpty();
		assertThat(value(paths.get("/api/v1/auth/me").get("get"), "security", List.class))
			.containsExactly(Map.of("cookieAuth", List.of()));
		mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
	}

	@Test
	void shouldDescribeGroupedHttpContractsWithoutApplicationDtos() throws Exception {
		Map<String, Object> document = document();
		Map<String, Object> components = value(document, "components");
		Map<String, Map<String, Object>> schemas = value(components, "schemas");
		assertThat(schemas).doesNotContainKeys("CreateWorkOrderInput", "WorkOrderOutput", "PricingPreviewOutput",
				"OccurrenceHistoryOutput", "AssignmentOutput", "Work", "Payment", "Assignment", "Settlement");
		assertThat(value(schemas.get("CreateWorkOrderRequest"), "properties", Map.class))
			.containsOnlyKeys("serviceDate", "conditions", "acceptedPricingFingerprint");
		assertThat(value(schemas.get("WorkOrderResponse"), "properties", Map.class))
			.containsKeys("customer", "schedule", "pricing", "recurrence", "assignments")
			.doesNotContainKeys("customerId", "totalAmount", "recurrenceSeriesId");
		assertThat(value(schemas.get("SeriesResponse"), "properties", Map.class))
			.containsOnlyKeys("id", "frequency", "period", "conditions", "lineage");
		assertThat(value(schemas.get("PricingPreviewResponse"), "properties", Map.class))
			.containsKeys("pricing", "summary", "participants").doesNotContainKeys("baseTotal", "totalAmount");
		assertThat(value(schemas.get("PricingProblemResponse"), "properties", Map.class))
			.containsKeys("code", "pricingPreview");
		assertThat(value(schemas.get("PaymentResponse"), "properties", Map.class))
			.containsKeys("recording", "reversal").doesNotContainKeys("recordedAt", "reversedBy");
		for (String report : List.of("WorkOrders", "CustomerPayments", "Collaborators")) {
			assertThat(value(schemas.get(report + "ReportResponse"), "properties", Map.class))
				.containsKeys("period", "context", "summary").doesNotContainKeys("from", "timezone");
		}
		assertThat(schemas).containsKeys("CustomerReferenceResponse", "CollaboratorReferenceResponse",
				"WorkAssignmentResponse", "CollaboratorsReportAssignmentResponse",
				"CustomerPaymentsReportPaymentResponse");
		Map<String, Map<String, Map<String, Object>>> paths = value(document, "paths");
		for (String path : List.of("/api/v1/work-orders", "/api/v1/recurrence-series")) {
			Map<String, Map<String, Object>> responses = value(paths.get(path).get("post"), "responses");
			assertThat(responses.get("422").toString()).contains("PricingProblemResponse");
			assertThat(responses.get("409").toString()).contains("PricingProblemResponse");
		}
		assertSchemaReferencesResolve(document, schemas);
	}

	@Test
	void shouldSupportTheDocumentedCookieAndCsrfFlow() throws Exception {
		var csrf = csrfResponse();
		String email = "swagger-" + java.util.UUID.randomUUID() + "@example.com";
		mvc.perform(post("/api/v1/accounts").cookie(csrf.getCookie("XSRF-TOKEN"))
			.header("X-XSRF-TOKEN", csrfToken(csrf))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Swagger account\",\"timezone\":\"Europe/London\",\"email\":\"" + email
					+ "\",\"password\":\"password123456\"}"))
			.andExpect(status().isCreated());
		var login = mvc
			.perform(post("/api/v1/auth/login").cookie(csrf.getCookie("XSRF-TOKEN"))
				.header("X-XSRF-TOKEN", csrfToken(csrf))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", email)
				.param("password", "password123456"))
			.andExpect(status().isNoContent())
			.andExpect(cookie().httpOnly("MOPLY_AUTH", true))
			.andReturn()
			.getResponse();
		Cookie auth = login.getCookie("MOPLY_AUTH");
		mvc.perform(get("/api/v1/auth/me").cookie(auth))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email));
		mvc.perform(post("/api/v1/customers").cookie(auth)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Swagger customer\"}")).andExpect(status().isForbidden());
		csrf = csrfResponse();
		mvc.perform(post("/api/v1/customers").cookie(auth, csrf.getCookie("XSRF-TOKEN"))
			.header("X-XSRF-TOKEN", csrfToken(csrf))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Swagger customer\"}")).andExpect(status().isCreated());
		mvc.perform(post("/api/v1/auth/logout").cookie(auth, csrf.getCookie("XSRF-TOKEN"))
			.header("X-XSRF-TOKEN", csrfToken(csrf)))
			.andExpect(status().isNoContent())
			.andExpect(cookie().maxAge("MOPLY_AUTH", 0));
		mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
	}

	private MockHttpServletResponse csrfResponse() throws Exception {
		return mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
	}

	private String csrfToken(MockHttpServletResponse response) throws Exception {
		return JsonPath.read(response.getContentAsString(), "$.token");
	}

	private Map<String, Object> document() throws Exception {
		String json = mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(json, "$");
	}

	private Map<String, Object> parameter(List<Map<String, Object>> parameters, String name) {
		return value(
				parameters.stream().filter(parameter -> name.equals(parameter.get("name"))).findFirst().orElseThrow(),
				"schema");
	}

	private void assertNoSwaggerAnnotations(AnnotatedElement element) {
		assertThat(element.getAnnotations()).noneMatch(
				annotation -> annotation.annotationType().getPackageName().startsWith("io.swagger.v3.oas.annotations"));
	}

	private void assertSchemaReferencesResolve(Object value, Map<String, ?> schemas) {
		if (value instanceof Map<?, ?> map) {
			if (map.get("$ref") instanceof String ref && ref.startsWith("#/components/schemas/")) {
				assertThat(schemas).containsKey(ref.substring("#/components/schemas/".length()));
			}
			map.values().forEach(child -> assertSchemaReferencesResolve(child, schemas));
		}
		else if (value instanceof List<?> list) {
			list.forEach(child -> assertSchemaReferencesResolve(child, schemas));
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T value(Map<String, Object> map, String key) {
		return (T) map.get(key);
	}

	private <T> T value(Map<String, Object> map, String key, Class<T> type) {
		return type.cast(map.get(key));
	}

}
