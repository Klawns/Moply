package com.klaus.moply.accounts.infra.web;

import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.klaus.moply.accounts.application.usecase.exception.AccountConflictException;
import com.klaus.moply.accounts.application.ports.AccountRegistration;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.auth.infra.security.JwtCookieService;
import com.klaus.moply.factory.PostgresSpringIntegrationTest;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = { "spring.jpa.open-in-view=false", "spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
// with(csrf()) in other suites replaces the cached filter's token repository.
// These tests exercise real cookies and must start with the production repository.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AccountApiIntegrationTest extends PostgresSpringIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwords;

	@Autowired
	AccountRegistration registration;

	private static final String PASSWORD = "long-test-password";

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM tb_auth_rate_bucket");
		jdbc.update("DELETE FROM tb_revoked_token");
		jdbc.update("DELETE FROM tb_work_assignment");
		jdbc.update("DELETE FROM tb_order_service");
		jdbc.update("DELETE FROM tb_customer_location");
		jdbc.update("DELETE FROM tb_customer");
		jdbc.update("DELETE FROM tb_collaborator");
		jdbc.update("DELETE FROM tb_app_user");
		jdbc.update("DELETE FROM tb_organization");
	}

	@Test
	void shouldRegisterLoginAndLogoutWithCookieJwtAndRealCsrf() throws Exception {
		var token = csrf(null);
		var signup = register(token, " Owner@EXAMPLE.com ").andExpect(status().isCreated()).andReturn();
		var encoded = jdbc.queryForObject("SELECT password_hash FROM tb_app_user", String.class);
		assertNotEquals(PASSWORD, encoded);
		assertTrue(passwords.matches(PASSWORD, encoded));
		var login = mvc
			.perform(post("/api/v1/auth/login").cookie(token.cookie())
				.header(token.header(), token.value())
				.param("email", "OWNER@example.com")
				.param("password", PASSWORD))
			.andExpect(status().isNoContent())
			.andExpect(header().doesNotExist("Location"))
			.andReturn();
		assertNull(login.getRequest().getSession(false));
		Cookie auth = login.getResponse().getCookie(JwtCookieService.COOKIE);
		assertNotNull(auth);
		assertTrue(auth.isHttpOnly());
		assertEquals("/api/v1", auth.getPath());
		assertEquals("Lax", auth.getAttribute("SameSite"));
		assertEquals(1800, auth.getMaxAge());
		assertEquals(0, login.getResponse().getCookie("XSRF-TOKEN").getMaxAge());
		mvc.perform(get("/api/v1/auth/me").cookie(auth))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.organizationId").value(json(signup, "$.organizationId")))
			.andExpect(jsonPath("$.email").value("owner@example.com"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.jwt").doesNotExist());
		mvc.perform(get("/api/v1/accounts/me").cookie(auth))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.currencyCode").value("GBP"))
			.andExpect(jsonPath("$.defaultWorkStatus").value("SCHEDULED"));
		var fresh = csrf(auth);
		var logout = mvc
			.perform(post("/api/v1/auth/logout").cookie(auth, fresh.cookie()).header(fresh.header(), fresh.value()))
			.andExpect(status().isNoContent())
			.andReturn();
		assertEquals(0, logout.getResponse().getCookie(JwtCookieService.COOKIE).getMaxAge());
		assertEquals(0, logout.getResponse().getCookie("XSRF-TOKEN").getMaxAge());
		mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
		// Logout revokes copied credentials across server instances.
		mvc.perform(get("/api/v1/auth/me").cookie(auth)).andExpect(status().isUnauthorized());
	}

	@Test
	void shouldThrottleLoginInTheActualSecurityChainBeforePasswordVerification() throws Exception {
		var token = csrf(null);
		for (int i = 0; i < 5; i++) {
			mvc.perform(post("/api/v1/auth/login").cookie(token.cookie())
				.header(token.header(), token.value())
				.param("email", i % 2 == 0 ? " throttle@example.invalid " : "THROTTLE@EXAMPLE.INVALID")
				.param("password", "invalid")
				.header("X-Forwarded-For", "192.0.2." + i)).andExpect(status().isUnauthorized());
		}
		mvc.perform(post("/api/v1/auth/login").cookie(token.cookie())
			.header(token.header(), token.value())
			.param("email", "throttle@example.invalid")
			.param("password", "invalid"))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
		mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk());
	}

	@Test
	void shouldThrottlePublicRegistrationWithItsOwnBudget() throws Exception {
		var token = csrf(null);
		for (int i = 0; i < 3; i++)
			register(token, "bad-email").andExpect(status().isBadRequest());
		register(token, "bad-email").andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
	}

	@Test
	void shouldRollbackOrganizationWhenDuplicateNormalizedEmailFails() throws Exception {
		var token = csrf(null);
		register(token, "owner@example.com").andExpect(status().isCreated());
		register(token, " OWNER@EXAMPLE.COM ").andExpect(status().isConflict());
		assertEquals(1, count("tb_organization"));
		assertEquals(1, count("tb_app_user"));
	}

	@Test
	void shouldEnforceOneManagerPerOrganizationAndForeignKeyInPostgres() throws Exception {
		register(csrf(null), "owner@example.com").andExpect(status().isCreated());
		UUID organization = jdbc.queryForObject("SELECT id FROM tb_organization", UUID.class);
		assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
				"INSERT INTO tb_app_user (id, organization_id, email, password_hash) VALUES (?, ?, 'second@example.com', 'encoded')",
				UUID.randomUUID(), organization));
		assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
				"INSERT INTO tb_app_user (id, organization_id, email, password_hash) VALUES (?, ?, 'other@example.com', 'encoded')",
				UUID.randomUUID(), UUID.randomUUID()));
		assertEquals(1, count("tb_app_user"));
	}

	@Test
	void shouldAllowOnlyOneConcurrentRegistrationForSameEmail() throws Exception {
		var ready = new java.util.concurrent.CountDownLatch(2);
		var start = new java.util.concurrent.CountDownLatch(1);
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			java.util.concurrent.Callable<Boolean> attempt = () -> {
				var organization = Organization.create("Empresa", "UTC");
				var manager = new AppUser(UUID.randomUUID(), organization.id(), new LoginEmail("race@example.com"),
						"encoded-fixture");
				ready.countDown();
				if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
					throw new IllegalStateException("Start timeout");
				}
				try {
					registration.register(organization, manager);
					return true;
				}
				catch (AccountConflictException exception) {
					return false;
				}
			};
			var first = executor.submit(attempt);
			var second = executor.submit(attempt);
			assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
			start.countDown();
			assertNotEquals(first.get(20, java.util.concurrent.TimeUnit.SECONDS),
					second.get(20, java.util.concurrent.TimeUnit.SECONDS));
		}
		assertEquals(1, count("tb_organization"));
		assertEquals(1, count("tb_app_user"));
	}

	@Test
	void shouldIsolateEveryExistingOperationalRoute() throws Exception {
		var a = signupAndLogin("a@example.com");
		var b = signupAndLogin("b@example.com");
		var ta = csrf(a);
		var tb = csrf(b);
		String accountB = json(mvc.perform(get("/api/v1/auth/me").cookie(b)).andReturn(), "$.organizationId");
		String ca = customer(a, ta, accountB);
		String cb = customer(b, tb, accountB);
		String la = json(mvc.perform(get("/api/v1/customers/" + ca).cookie(a)).andReturn(), "$.locations[0].id");
		String lb = json(mvc.perform(get("/api/v1/customers/" + cb).cookie(b)).andReturn(), "$.locations[0].id");
		String oa = order(a, ta, ca);
		String ob = order(b, tb, cb);
		mvc.perform(get("/api/v1/locations").cookie(a))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(la))
			.andExpect(jsonPath("$.content[0].customerId").value(ca));
		for (String path : new String[] { "/api/v1/customers", "/api/v1/locations", "/api/v1/customers/" + ca,
				"/api/v1/customers/" + ca + "/locations", "/api/v1/work-orders/" + oa }) {
			mvc.perform(get(path)).andExpect(status().isUnauthorized());
		}
		mvc.perform(get("/api/v1/customers").cookie(a))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(ca));
		for (String path : new String[] { "/api/v1/customers/" + cb, "/api/v1/customers/" + cb + "/locations",
				"/api/v1/customers/" + cb + "/locations/" + lb, "/api/v1/customers/" + ca + "/locations/" + lb,
				"/api/v1/work-orders/" + ob }) {
			mvc.perform(get(path).cookie(a)).andExpect(status().isNotFound());
		}
		for (String path : new String[] { "/api/v1/customers/" + cb, "/api/v1/customers/" + cb + "/locations/" + lb,
				"/api/v1/customers/" + ca + "/locations/" + lb }) {
			mvc.perform(put(path).cookie(a, ta.cookie())
				.header(ta.header(), ta.value())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Changed\"}")).andExpect(status().isNotFound());
		}
		mvc.perform(post("/api/v1/customers/" + cb + "/locations").cookie(a, ta.cookie())
			.header(ta.header(), ta.value())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Wrong\"}")).andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/work-orders").cookie(a, ta.cookie())
			.header(ta.header(), ta.value())
			.contentType(MediaType.APPLICATION_JSON)
			.content(orderBody(cb))).andExpect(status().isNotFound());
		mvc.perform(delete("/api/v1/work-orders/" + ob).cookie(a, ta.cookie()).header(ta.header(), ta.value()))
			.andExpect(status().isMethodNotAllowed());
		for (boolean byName : new boolean[] { false, true }) {
			var request = get("/api/v1/work-orders").cookie(a).param("from", "2026-09-28").param("to", "2026-09-28");
			if (byName)
				request.param("customerId", ca);
			mvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value(oa))
				.andExpect(jsonPath("$.totalElements").value(1));
		}
		mvc.perform(put("/api/v1/customers/" + ca + "/locations/" + la).cookie(a, ta.cookie())
			.header(ta.header(), ta.value())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Updated\"}")).andExpect(status().isOk());
		mvc.perform(get("/api/v1/customers/" + cb + "/locations/" + lb).cookie(b))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Casa"));
		mvc.perform(delete("/api/v1/work-orders/" + oa).cookie(a, ta.cookie()).header(ta.header(), ta.value()))
			.andExpect(status().isMethodNotAllowed());
		mvc.perform(get("/api/v1/work-orders/" + ob).cookie(b)).andExpect(status().isOk());
	}

	@Test
	void shouldAcceptAuthenticationOnlyFromCookie() throws Exception {
		var auth = signupAndLogin("owner@example.com");
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + auth.getValue()))
			.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/auth/me").param("access_token", auth.getValue()).param("token", auth.getValue()))
			.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/auth/me").contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"" + auth.getValue() + "\"}")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/auth/me").cookie(new Cookie(JwtCookieService.COOKIE, "invalid")))
			.andExpect(status().isUnauthorized());
		String[] parts = auth.getValue().split("\\.");
		String altered = java.util.Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString("{\"sub\":\"fake\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		mvc.perform(get("/api/v1/auth/me")
			.cookie(new Cookie(JwtCookieService.COOKIE, parts[0] + "." + altered + "." + parts[2])))
			.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/auth/me").cookie(auth, auth)).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/auth/me").cookie(auth).header("Authorization", "Bearer ignored"))
			.andExpect(status().isOk());
	}

	@Test
	void shouldRequireCsrfForEveryOperationalMutationAndIgnoreOldSessions() throws Exception {
		var auth = signupAndLogin("owner@example.com");
		var token = csrf(auth);
		String customer = customer(auth, token, UUID.randomUUID().toString());
		String location = json(mvc.perform(get("/api/v1/customers/" + customer).cookie(auth)).andReturn(),
				"$.locations[0].id");
		String order = order(auth, token, customer);
		String[][] mutations = { { "POST", "/api/v1/customers" }, { "PUT", "/api/v1/customers/" + customer },
				{ "POST", "/api/v1/customers/" + customer + "/locations" },
				{ "PUT", "/api/v1/customers/" + customer + "/locations/" + location },
				{ "POST", "/api/v1/work-orders" }, { "DELETE", "/api/v1/work-orders/" + order } };
		for (var mutation : mutations) {
			var method = org.springframework.http.HttpMethod.valueOf(mutation[0]);
			mvc.perform(request(method, mutation[1]).cookie(auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
			mvc.perform(request(method, mutation[1]).cookie(auth, token.cookie())
				.header(token.header(), "wrong")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")).andExpect(status().isForbidden());
			mvc.perform(request(method, mutation[1]).cookie(token.cookie())
				.header(token.header(), token.value())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")).andExpect(status().isUnauthorized());
		}
		var session = new org.springframework.mock.web.MockHttpSession();
		var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
		var user = jdbc.queryForObject("SELECT id FROM tb_app_user", UUID.class);
		var account = jdbc.queryForObject("SELECT id FROM tb_organization", UUID.class);
		var principal = new com.klaus.moply.auth.infra.security.AccountPrincipal(
				new AppUser(user, account, new LoginEmail("owner@example.com"), "encoded"));
		context.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken
			.authenticated(principal, null, principal.getAuthorities()));
		session.setAttribute("SPRING_SECURITY_CONTEXT", context);
		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
	}

	private String customer(Cookie auth, Token csrf, String account) throws Exception {
		return mvc
			.perform(post("/api/v1/customers").cookie(auth, csrf.cookie())
				.header(csrf.header(), csrf.value())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Maria\",\"organizationId\":\"" + account
						+ "\",\"locations\":[{\"name\":\"Casa\"}]}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString()
			.replace("\"", "");
	}

	private String order(Cookie auth, Token csrf, String customer) throws Exception {
		String participant = json(mvc
			.perform(post("/api/v1/collaborators").cookie(auth, csrf.cookie())
				.header(csrf.header(), csrf.value())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Worker\"}"))
			.andExpect(status().isCreated())
			.andReturn(), "$");
		String input = orderBody(customer).replace("00000000-0000-0000-0000-000000000099", participant);
		String fingerprint = json(mvc
			.perform(post("/api/v1/work-orders/pricing-preview").cookie(auth, csrf.cookie())
				.header(csrf.header(), csrf.value())
				.contentType(MediaType.APPLICATION_JSON)
				.content(input))
			.andExpect(status().isOk())
			.andReturn(), "$.pricingFingerprint");
		input = input.substring(0, input.length() - 1) + ",\"acceptedPricingFingerprint\":\"" + fingerprint + "\"}";
		return json(mvc
			.perform(post("/api/v1/work-orders").cookie(auth, csrf.cookie())
				.header(csrf.header(), csrf.value())
				.contentType(MediaType.APPLICATION_JSON)
				.content(input))
			.andExpect(status().isCreated())
			.andReturn(), "$.id");
	}

	private String orderBody(String customer) {
		return "{\"serviceDate\":\"2026-09-28\",\"conditions\":{\"customerId\":\"" + customer
				+ "\",\"contractedHours\":4,\"hourlyRate\":10,\"participantIds\":[\"00000000-0000-0000-0000-000000000099\"]}}";
	}

	@Test
	void shouldRejectMissingAndInvalidCsrfIncludingSignupAndLogin() throws Exception {
		mvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(signupBody("a@b")))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/auth/login").param("email", "a@b").param("password", PASSWORD))
			.andExpect(status().isForbidden());
		var session = signupAndLogin("owner@example.com");
		var foreignToken = csrf(null);
		mvc.perform(post("/api/v1/auth/logout").cookie(session).header(foreignToken.header(), foreignToken.value()))
			.andExpect(status().isForbidden());
		mvc.perform(put("/api/v1/accounts/me/preferences").cookie(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"timezone\":\"UTC\",\"defaultWorkStatus\":\"COMPLETED\"}")).andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/auth/logout").cookie(session).header("X-CSRF-TOKEN", "wrong"))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isOk());
	}

	@Test
	void shouldRejectWrongCredentialsWithoutRedirectOrSessionAuthentication() throws Exception {
		var token = csrf(null);
		register(token, "owner@example.com").andExpect(status().isCreated());
		for (String email : new String[] { "owner@example.com", "missing@example.com", "invalid" }) {
			mvc.perform(post("/api/v1/auth/login").cookie(token.cookie())
				.header(token.header(), token.value())
				.param("email", email)
				.param("password", "wrong"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(jsonPath("$.title").value("Credenciais inválidas."));
			mvc.perform(get("/api/v1/auth/me").cookie(token.cookie())).andExpect(status().isUnauthorized());
		}
	}

	@Test
	void shouldUpdateOnlyAuthenticatedAccountsPreferences() throws Exception {
		var first = signupAndLogin("first@example.com");
		var second = signupAndLogin("second@example.com");
		String secondId = json(mvc.perform(get("/api/v1/auth/me").cookie(second)).andReturn(), "$.organizationId");
		var token = csrf(first);
		mvc.perform(put("/api/v1/accounts/me/preferences").cookie(first, token.cookie())
			.header(token.header(), token.value())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"organizationId":"%s","timezone":"America/Sao_Paulo","defaultWorkStatus":"COMPLETED"}
					""".formatted(secondId))).andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/accounts/me/preferences").cookie(first))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
			.andExpect(jsonPath("$.defaultWorkStatus").value("COMPLETED"));
		mvc.perform(get("/api/v1/accounts/me/preferences").cookie(second))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.timezone").value("Europe/London"))
			.andExpect(jsonPath("$.defaultWorkStatus").value("SCHEDULED"));

		for (Cookie auth : new Cookie[] { first, second }) {
			var preferences = mvc.perform(get("/api/v1/accounts/me/preferences").cookie(auth))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
			mvc.perform(get("/api/v1/accounts/me").cookie(auth))
				.andExpect(status().isOk())
				.andExpect(content().json(preferences, JsonCompareMode.STRICT));
		}
	}

	@Test
	void shouldReturnNotFoundForPreferencesOfMissingAccount() throws Exception {
		// Exercise the HTTP exception mapping with an authenticated principal whose
		// account is absent, without bypassing the preference use cases.
		var principal = new AccountPrincipal(new AppUser(UUID.randomUUID(), UUID.randomUUID(),
				new LoginEmail("missing@example.com"), "encoded-fixture"));
		for (String path : new String[] { "/api/v1/accounts/me", "/api/v1/accounts/me/preferences" }) {
			mvc.perform(get(path).with(user(principal)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Conta não encontrada."))
				.andExpect(jsonPath("$.category").value("APPLICATION_ERROR"))
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
		}
		var token = csrf(null);
		mvc.perform(put("/api/v1/accounts/me/preferences").with(user(principal))
			.cookie(token.cookie())
			.header(token.header(), token.value())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"timezone\":\"UTC\",\"defaultWorkStatus\":\"COMPLETED\"}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Conta não encontrada."))
			.andExpect(jsonPath("$.category").value("APPLICATION_ERROR"))
			.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void shouldRejectInvalidTimezoneStatusAndSignupWithoutPersisting() throws Exception {
		var token = csrf(null);
		mvc.perform(post("/api/v1/accounts").cookie(token.cookie())
			.header(token.header(), token.value())
			.contentType(MediaType.APPLICATION_JSON)
			.content(signupBody("a@b").replace("Europe/London", "Invalid/Zone"))).andExpect(status().isBadRequest());
		assertEquals(0, count("tb_organization"));
		assertEquals(0, count("tb_app_user"));
		var session = signupAndLogin("owner@example.com");
		var authenticated = csrf(session);
		mvc.perform(put("/api/v1/accounts/me/preferences").cookie(session, authenticated.cookie())
			.header(authenticated.header(), authenticated.value())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"timezone\":\"UTC\",\"defaultWorkStatus\":\"CANCELLED\"}")).andExpect(status().isBadRequest());
	}

	@Autowired
	com.klaus.moply.customers.application.ports.CustomerRepository customers;

	@Autowired
	com.klaus.moply.workorders.application.ports.WorkOrderRepository orders;

	@Autowired
	com.klaus.moply.collaborators.application.ports.CollaboratorRepository collaborators;

	@Test
	void shouldIsolateRepositoryWritesAndConcurrentLocationChanges() throws Exception {
		var a = signupAndLogin("a@example.com");
		var b = signupAndLogin("b@example.com");
		UUID aid = UUID.fromString(json(mvc.perform(get("/api/v1/auth/me").cookie(a)).andReturn(), "$.organizationId"));
		UUID bid = UUID.fromString(json(mvc.perform(get("/api/v1/auth/me").cookie(b)).andReturn(), "$.organizationId"));
		var original = customers.save(aid, com.klaus.moply.customers.domain.entities.Customer.create("Maria"));
		assertEquals(0, customers.findAll(bid, new com.klaus.moply.shared.application.pagination.PageQuery(0, 20, null))
			.totalElements());
		assertTrue(customers.findById(bid, original.getId()).isEmpty());
		assertThrows(com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException.class,
				() -> customers.save(bid, original.update("Wrong", null, null, null)));
		var participant = collaborators.save(aid,
				com.klaus.moply.collaborators.domain.entities.Collaborator.create(aid, "Worker", null));
		var order = com.klaus.moply.factory.WorkOrderFactory.create(original.getId(), participant.getId());
		assertThrows(com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException.class,
				() -> orders.save(bid, order));
		var savedOrder = orders.save(aid, order);
		assertTrue(orders.findById(bid, savedOrder.id()).isEmpty());
		assertTrue(orders.findAll(bid, new WorkOrderDateRange(null, null), original.getId()).isEmpty());
		assertTrue(orders.findAll(bid, new WorkOrderDateRange(savedOrder.serviceDate(), savedOrder.serviceDate()), null)
			.isEmpty());
		assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class,
				() -> orders.save(bid, savedOrder));
		var ready = new java.util.concurrent.CountDownLatch(2);
		var start = new java.util.concurrent.CountDownLatch(1);
		try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			java.util.concurrent.Callable<Boolean> edit = () -> {
				var snapshot = customers.findById(aid, original.getId()).orElseThrow();
				ready.countDown();
				if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS))
					throw new IllegalStateException("Timeout");
				try {
					customers.save(aid, snapshot.addLocation(
							com.klaus.moply.customers.domain.entities.CustomerLocation.create("Casa", null, null)));
					return true;
				}
				catch (org.springframework.dao.OptimisticLockingFailureException exception) {
					return false;
				}
			};
			var first = executor.submit(edit);
			var second = executor.submit(edit);
			assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
			start.countDown();
			assertNotEquals(first.get(20, java.util.concurrent.TimeUnit.SECONDS),
					second.get(20, java.util.concurrent.TimeUnit.SECONDS));
		}
		var updated = customers.findById(aid, original.getId()).orElseThrow();
		assertEquals(1, updated.getLocations().size());
		assertTrue(updated.getVersion() > original.getVersion());
		var retained = customers.save(aid, updated.update("Renamed", null, null, null));
		assertEquals(1, retained.getLocations().size());
	}

	private Cookie signupAndLogin(String email) throws Exception {
		var token = csrf(null);
		register(token, email).andExpect(status().isCreated());
		return mvc
			.perform(post("/api/v1/auth/login").cookie(token.cookie())
				.header(token.header(), token.value())
				.param("email", email)
				.param("password", PASSWORD))
			.andExpect(status().isNoContent())
			.andReturn()
			.getResponse()
			.getCookie(JwtCookieService.COOKIE);
	}

	private org.springframework.test.web.servlet.ResultActions register(Token token, String email) throws Exception {
		return mvc.perform(post("/api/v1/accounts").cookie(token.cookie())
			.header(token.header(), token.value())
			.contentType(MediaType.APPLICATION_JSON)
			.content(signupBody(email)));
	}

	private String signupBody(String email) {
		return """
				{"name":"Empresa","timezone":"Europe/London","email":"%s","password":"%s"}
				""".formatted(email, PASSWORD);
	}

	private Token csrf(Cookie session) throws Exception {
		var request = get("/api/v1/auth/csrf");
		if (session != null) {
			request.cookie(session);
		}
		var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
		var cookie = result.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(cookie, "GET /api/v1/auth/csrf deve emitir o cookie XSRF-TOKEN real.");
		return new Token(cookie, json(result, "$.headerName"), json(result, "$.token"));
	}

	private String json(MvcResult result, String path) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), path);
	}

	private int count(String table) {
		return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
	}

	private record Token(Cookie cookie, String header, String value) {
	}

}
