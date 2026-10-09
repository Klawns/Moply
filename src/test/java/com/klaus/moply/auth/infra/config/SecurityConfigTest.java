package com.klaus.moply.auth.infra.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.auth.infra.security.AuthRateLimitStore;
import java.time.Clock;
import com.klaus.moply.auth.infra.security.JwtCookieService;
import com.klaus.moply.auth.infra.web.AuthController;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import({ SecurityConfig.class, SecurityConfigTest.WebSecurity.class })
class SecurityConfigTest {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	JwtCookieService jwt;

	@MockitoBean
	AuthRateLimitStore rateLimits;

	@MockitoBean
	UserDetailsService users;

	@Test
	void shouldDenyDocumentationWhenDisabled() throws Exception {
		for (String path : new String[] { "/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html",
				"/swagger-ui/index.html" }) {
			mvc.perform(get(path)).andExpect(status().isUnauthorized());
			mvc.perform(get(path).with(user("manager"))).andExpect(status().isForbidden());
		}
	}

	@Test
	void shouldExposeCsrfTokenWithHttpOnlyCookie() throws Exception {
		mvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(cookie().httpOnly("XSRF-TOKEN", true))
			.andExpect(cookie().path("XSRF-TOKEN", "/api/v1"));
	}

	@Test
	void shouldRequireAuthenticationAndDenyUnlistedRoutes() throws Exception {
		mvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Autenticação necessária."))
			.andExpect(jsonPath("$.category").value("SECURITY_ERROR"))
			.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			.andExpect(jsonPath("$.requestId").isNotEmpty())
			.andExpect(header().exists("X-Request-ID"));
		var principal = new AccountPrincipal(
				new AppUser(UUID.randomUUID(), UUID.randomUUID(), new LoginEmail("owner@example.com"), "encoded"));
		mvc.perform(get("/api/v1/auth/me").with(user(principal)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(principal.getUserId().toString()))
			.andExpect(jsonPath("$.organizationId").value(principal.getOrganizationId().toString()))
			.andExpect(jsonPath("$.email").value("owner@example.com"));
		mvc.perform(get("/unlisted").with(user("manager")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.title").value("Acesso negado."))
			.andExpect(jsonPath("$.category").value("SECURITY_ERROR"))
			.andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
			.andExpect(jsonPath("$.requestId").isNotEmpty())
			.andExpect(header().exists("X-Request-ID"));
	}

	@Test
	void shouldRequireCsrfEvenForPublicMutations() throws Exception {
		for (String path : new String[] { "/api/v1/accounts", "/api/v1/auth/login", "/api/v1/auth/logout" }) {
			mvc.perform(post(path)).andExpect(status().isForbidden());
		}
		verifyNoInteractions(jwt, users);
	}

	@Test
	void shouldLoginWithCookieWithoutCreatingSessionAndRejectWrongPassword() throws Exception {
		var principal = new AccountPrincipal(new AppUser(UUID.randomUUID(), UUID.randomUUID(),
				new LoginEmail("owner@example.com"), "{noop}password"));
		when(users.loadUserByUsername("owner@example.com")).thenReturn(principal);
		when(jwt.issue(principal)).thenReturn("token");
		when(jwt.cookie("token")).thenReturn(ResponseCookie.from(JwtCookieService.COOKIE, "token").build());

		var result = mvc
			.perform(postWithCsrf("/api/v1/auth/login").param("email", "owner@example.com")
				.param("password", "password"))
			.andExpect(status().isNoContent())
			.andExpect(cookie().value(JwtCookieService.COOKIE, "token"))
			.andReturn();
		assertNull(result.getRequest().getSession(false));
		mvc.perform(postWithCsrf("/api/v1/auth/login").param("email", "owner@example.com").param("password", "wrong"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Credenciais inválidas."))
			.andExpect(jsonPath("$.category").value("SECURITY_ERROR"))
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.requestId").isNotEmpty())
			.andExpect(header().exists("X-Request-ID"));
	}

	@Test
	void shouldClearAuthenticationCookieOnLogout() throws Exception {
		when(jwt.cookie("")).thenReturn(ResponseCookie.from(JwtCookieService.COOKIE, "").maxAge(0).build());
		mvc.perform(postWithCsrf("/api/v1/auth/logout"))
			.andExpect(status().isNoContent())
			.andExpect(cookie().maxAge(JwtCookieService.COOKIE, 0));
	}

	@Test
	void shouldNotReportLogoutSuccessWhenRevocationStorageFails() throws Exception {
		org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"))
			.when(jwt)
			.revoke("copied-token");
		mvc.perform(postWithCsrf("/api/v1/auth/logout")
			.cookie(new jakarta.servlet.http.Cookie(JwtCookieService.COOKIE, "copied-token")))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.code").value("AUTH_STORAGE_UNAVAILABLE"));
	}

	@Test
	void shouldRejectAmbiguousLogoutCookiesWithoutRevokingEitherToken() throws Exception {
		mvc.perform(postWithCsrf("/api/v1/auth/logout").cookie(
				new jakarta.servlet.http.Cookie(JwtCookieService.COOKIE, "first"),
				new jakarta.servlet.http.Cookie(JwtCookieService.COOKIE, "second")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_AUTH_COOKIE"));
		org.mockito.Mockito.verify(jwt, org.mockito.Mockito.never()).revoke(org.mockito.ArgumentMatchers.anyString());
	}

	private MockHttpServletRequestBuilder postWithCsrf(String path) throws Exception {
		var response = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
		return post(path).cookie(response.getCookie("XSRF-TOKEN"))
			.header(JsonPath.<String>read(response.getContentAsString(), "$.headerName"),
					JsonPath.<String>read(response.getContentAsString(), "$.token"));
	}

	@TestConfiguration
	@EnableWebSecurity
	static class WebSecurity {

		@org.springframework.context.annotation.Bean
		Clock clock() {
			return Clock.systemUTC();
		}

	}

}
