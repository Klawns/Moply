package com.klaus.moply.collaborators.infra.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.klaus.moply.auth.infra.security.JwtCookieService;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.collaborators.application.usecase.FindEligibleCollaborators;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.factory.CollaboratorFactory;
import com.klaus.moply.factory.PostgresSpringIntegrationTest;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = { "spring.jpa.open-in-view=false", "spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
// with(csrf()) in other suites replaces the cached filter's token repository.
// These tests exercise real cookies and must start with the production repository.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CollaboratorApiIntegrationTest extends PostgresSpringIntegrationTest {

	private static final String BASE = "/api/v1/collaborators";

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	CollaboratorRepository repo;

	@Autowired
	FindEligibleCollaborators eligible;

	@BeforeEach
	void cleanUp() {
		jdbc.update("DELETE FROM tb_collaborator");
		jdbc.update("DELETE FROM tb_app_user");
		jdbc.update("DELETE FROM tb_organization");
	}

	@Test
	void shouldPersistPreserveAndRemoveOptionalHourlyRate() throws Exception {
		var owner = account("rate@example.com");
		var id = create(owner, "{\"name\":\"Ana\",\"hourlyRate\":20}");
		mvc.perform(get(BASE + "/" + id).cookie(owner.auth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hourlyRate").value(20));
		mvc.perform(mutation(put(BASE + "/" + id), owner).content("{\"name\":\"Ana renamed\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hourlyRate").value(20));
		for (String invalid : new String[] { "0", "-1", "1.001" }) {
			mvc.perform(
					mutation(put(BASE + "/" + id), owner).content("{\"name\":\"Ana\",\"hourlyRate\":" + invalid + "}"))
				.andExpect(status().isBadRequest());
		}
		mvc.perform(mutation(put(BASE + "/" + id), owner).content("{\"name\":\"Ana\",\"hourlyRate\":null}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hourlyRate").doesNotExist());
	}

	@Test
	void shouldManageHomonymsWithoutCredentialsAndPreserveInactiveHistory() throws Exception {
		var owner = account("owner@example.com");
		var usersBefore = jdbc.queryForList("SELECT * FROM tb_app_user");
		var id = create(owner,
				"{\"name\":\" Maria \",\"phone\":\" +44 (0) 123 \",\"email\":\"collaborator@example.com\",\"password\":\"not-a-login\",\"active\":false}");
		var homonym = create(owner, "{\"name\":\"Maria\"}");
		assertNotEquals(id, homonym);
		mvc.perform(get(BASE + "/" + id).cookie(owner.auth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Maria"))
			.andExpect(jsonPath("$.phone").value("+44 (0) 123"))
			.andExpect(jsonPath("$.active").value(true))
			.andExpect(jsonPath("$.email").doesNotExist())
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.userId").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
		mvc.perform(mutation(put(BASE + "/" + id), owner).content("{\"name\":\"Ana\",\"phone\":\" \"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id.toString()))
			.andExpect(jsonPath("$.phone").doesNotExist());
		for (int i = 0; i < 2; i++)
			mvc.perform(mutation(post(BASE + "/" + id + "/deactivate"), owner)).andExpect(status().isNoContent());
		mvc.perform(get(BASE + "/" + id).cookie(owner.auth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Ana"))
			.andExpect(jsonPath("$.active").value(false));
		mvc.perform(mutation(put(BASE + "/" + id), owner).content("{\"name\":\"Changed\"}"))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		mvc.perform(get(BASE).cookie(owner.auth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.totalElements").value(2));
		mvc.perform(get(BASE).param("active", "true").cookie(owner.auth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(homonym.toString()));
		mvc.perform(get(BASE).param("active", "false").cookie(owner.auth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(id.toString()))
			.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get(BASE).param("active", "false").param("page", "1").param("size", "1").cookie(owner.auth()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(1));
		assertEquals(homonym, eligible.execute(new Context(owner.id()), null).getFirst().id());
		assertEquals(usersBefore, jdbc.queryForList("SELECT * FROM tb_app_user"));
		var csrf = csrf(null);
		mvc.perform(post("/api/v1/auth/login").cookie(csrf.cookie())
			.header(csrf.header(), csrf.value())
			.param("email", "collaborator@example.com")
			.param("password", "not-a-login")).andExpect(status().isUnauthorized());
		mvc.perform(delete(BASE + "/" + homonym).cookie(owner.auth())).andExpect(status().isForbidden());
		assertEquals(2, repo.findAll(owner.id(), null, PageQuery.defaults()).totalElements());
	}

	@Test
	void shouldIsolateReadsUpdatesDeactivationAndForgedAccountInPayload() throws Exception {
		var a = account("a@example.com");
		var b = account("b@example.com");
		var ca = create(a, "{\"name\":\"Maria\",\"organizationId\":\"" + b.id() + "\"}");
		var cb = create(b, "{\"name\":\"Maria\"}");
		for (String filter : new String[] { "", "?active=true" }) {
			mvc.perform(get(BASE + filter).cookie(a.auth()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value(ca.toString()));
		}
		for (UUID id : new UUID[] { cb, UUID.randomUUID() }) {
			mvc.perform(get(BASE + "/" + id).cookie(a.auth())).andExpect(status().isNotFound());
			mvc.perform(mutation(put(BASE + "/" + id), a).content("{\"name\":\"Changed\"}"))
				.andExpect(status().isNotFound());
			mvc.perform(mutation(post(BASE + "/" + id + "/deactivate"), a)).andExpect(status().isNotFound());
		}
		mvc.perform(
				mutation(put(BASE + "/" + ca), a).content("{\"name\":\"Ana\",\"organizationId\":\"" + b.id() + "\"}"))
			.andExpect(status().isOk());
		assertEquals(a.id(), repo.findById(a.id(), ca).orElseThrow().getOrganizationId());
		assertTrue(repo.findById(b.id(), ca).isEmpty());
		var foreign = repo.findById(b.id(), cb).orElseThrow();
		assertThrows(CollaboratorNotFoundException.class, () -> repo.save(a.id(), foreign.update("Wrong", null)));
		assertThrows(CollaboratorNotFoundException.class, () -> repo.save(a.id(), foreign.deactivate()));
		var forged = Collaborator.restore(cb, a.id(), "Wrong", null, false, foreign.getVersion());
		assertThrows(CollaboratorNotFoundException.class, () -> repo.save(a.id(), forged));
		assertSameState(foreign, repo.findById(b.id(), cb).orElseThrow());
		assertEquals(ca, eligible.execute(new Context(a.id()), null).getFirst().id());
	}

	@Test
	void shouldValidateInputAndRequireCookieAndCsrfForAllMutations() throws Exception {
		var owner = account("owner@example.com");
		var id = create(owner, "{\"name\":\"Maria\"}");
		mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		mvc.perform(get(BASE + "/" + id)).andExpect(status().isUnauthorized());
		mvc.perform(get(BASE).header("Authorization", "Bearer " + owner.auth().getValue()))
			.andExpect(status().isUnauthorized());
		for (var mutation : new MockHttpServletRequestBuilder[] { post(BASE), put(BASE + "/" + id),
				post(BASE + "/" + id + "/deactivate") }) {
			mvc.perform(
					mutation.cookie(owner.auth()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
				.andExpect(status().isForbidden());
		}
		for (var mutation : new MockHttpServletRequestBuilder[] { post(BASE), put(BASE + "/" + id),
				post(BASE + "/" + id + "/deactivate") }) {
			mvc.perform(mutation.cookie(owner.auth(), owner.csrf().cookie())
				.header(owner.csrf().header(), "invalid")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ana\"}")).andExpect(status().isForbidden());
		}
		var anonymous = csrf(null);
		for (var mutation : new MockHttpServletRequestBuilder[] { post(BASE), put(BASE + "/" + id),
				post(BASE + "/" + id + "/deactivate") }) {
			mvc.perform(mutation.cookie(anonymous.cookie())
				.header(anonymous.header(), anonymous.value())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ana\"}")).andExpect(status().isUnauthorized());
		}
		for (String body : new String[] { "{}", "{\"name\":null}", "{\"name\":\" \"}", "{\"name\":\"\\t\\n\"}", "{" }) {
			mvc.perform(mutation(post(BASE), owner).content(body)).andExpect(status().isBadRequest());
			mvc.perform(mutation(put(BASE + "/" + id), owner).content(body)).andExpect(status().isBadRequest());
		}
		mvc.perform(get(BASE + "/invalid").cookie(owner.auth())).andExpect(status().isBadRequest());
		mvc.perform(get(BASE).param("active", "invalid").cookie(owner.auth())).andExpect(status().isBadRequest());
		assertEquals(1, repo.findAll(owner.id(), null, PageQuery.defaults()).totalElements());
		assertEquals("Maria", repo.findById(owner.id(), id).orElseThrow().getName().value());
	}

	@Test
	void shouldRejectStaleEditsAndNeverReactivateDuringConcurrentWrites() throws Exception {
		var owner = account("owner@example.com");
		var original = repo.save(owner.id(), CollaboratorFactory.create(owner.id()));
		var ready = new CountDownLatch(2);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Callable<Boolean> edit = () -> race(owner.id(), original.getId(), false, ready, start);
			Callable<Boolean> deactivate = () -> race(owner.id(), original.getId(), true, ready, start);
			var first = executor.submit(edit);
			var second = executor.submit(deactivate);
			assertTrue(ready.await(10, TimeUnit.SECONDS));
			start.countDown();
			assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
		}
		var current = repo.findById(owner.id(), original.getId()).orElseThrow();
		assertTrue(current.getVersion() > original.getVersion());
		if (current.isActive())
			repo.save(owner.id(), current.deactivate());
		assertThrows(OptimisticLockingFailureException.class,
				() -> repo.save(owner.id(), original.update("Stale", null)));
		var inactive = repo.findById(owner.id(), original.getId()).orElseThrow();
		var forged = Collaborator.restore(inactive.getId(), owner.id(), "Forged", null, true, inactive.getVersion());
		assertThrows(InactiveCollaboratorException.class, () -> repo.save(owner.id(), forged));
		assertSameState(inactive, repo.findById(owner.id(), original.getId()).orElseThrow());
		assertTrue(eligible.execute(new Context(owner.id()), null).isEmpty());
	}

	private void assertSameState(Collaborator expected, Collaborator actual) {
		assertEquals(expected.getId(), actual.getId());
		assertEquals(expected.getOrganizationId(), actual.getOrganizationId());
		assertEquals(expected.getName(), actual.getName());
		assertEquals(expected.getPhone(), actual.getPhone());
		assertEquals(expected.isActive(), actual.isActive());
		assertEquals(expected.getVersion(), actual.getVersion());
	}

	private boolean race(UUID account, UUID id, boolean deactivate, CountDownLatch ready, CountDownLatch start)
			throws Exception {
		var snapshot = repo.findById(account, id).orElseThrow();
		ready.countDown();
		if (!start.await(10, TimeUnit.SECONDS))
			throw new IllegalStateException("Timeout");
		try {
			repo.save(account, deactivate ? snapshot.deactivate() : snapshot.update("Ana", "123"));
			return true;
		}
		catch (OptimisticLockingFailureException exception) {
			return false;
		}
	}

	private MockHttpServletRequestBuilder mutation(MockHttpServletRequestBuilder request, Owner owner) {
		return request.cookie(owner.auth(), owner.csrf().cookie())
			.header(owner.csrf().header(), owner.csrf().value())
			.contentType(MediaType.APPLICATION_JSON);
	}

	private UUID create(Owner owner, String body) throws Exception {
		var result = mvc.perform(mutation(post(BASE), owner).content(body)).andExpect(status().isCreated()).andReturn();
		UUID id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$"));
		assertEquals(BASE + "/" + id, result.getResponse().getHeader("Location"));
		return id;
	}

	private Owner account(String email) throws Exception {
		var csrf = csrf(null);
		var signup = mvc
			.perform(post("/api/v1/accounts").cookie(csrf.cookie())
				.header(csrf.header(), csrf.value())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Account\",\"timezone\":\"Europe/London\",\"email\":\"" + email
						+ "\",\"password\":\"long-test-password\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		var login = mvc
			.perform(post("/api/v1/auth/login").cookie(csrf.cookie())
				.header(csrf.header(), csrf.value())
				.param("email", email)
				.param("password", "long-test-password"))
			.andExpect(status().isNoContent())
			.andReturn();
		var auth = login.getResponse().getCookie(JwtCookieService.COOKIE);
		assertNotNull(auth, "Login deve emitir o cookie de autenticação.");
		String id = JsonPath.read(signup.getResponse().getContentAsString(), "$.organizationId");
		return new Owner(UUID.fromString(id), auth, csrf(auth));
	}

	private Token csrf(Cookie auth) throws Exception {
		var request = get("/api/v1/auth/csrf");
		if (auth != null)
			request.cookie(auth);
		MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
		var cookie = result.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(cookie, "GET /api/v1/auth/csrf deve emitir o cookie XSRF-TOKEN real.");
		return new Token(cookie, JsonPath.read(result.getResponse().getContentAsString(), "$.headerName"),
				JsonPath.read(result.getResponse().getContentAsString(), "$.token"));
	}

	private record Token(Cookie cookie, String header, String value) {
	}

	private record Owner(UUID id, Cookie auth, Token csrf) {
	}

}
