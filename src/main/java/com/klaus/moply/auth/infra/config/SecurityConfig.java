package com.klaus.moply.auth.infra.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.auth.infra.security.JwtCookieFilter;
import com.klaus.moply.auth.infra.security.JwtCookieService;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	@Value("${springdoc.api-docs.enabled:false}")
	private boolean apiDocsEnabled;

	@Value("${springdoc.swagger-ui.enabled:false}")
	private boolean swaggerUiEnabled;

	private static final String LOGIN_URL = "/api/v1/auth/login";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtCookieService jwt,
			@Value("${moply.auth.cookie.secure:false}") boolean secure) throws Exception {
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
			.requestCache(AbstractHttpConfigurer::disable)
			.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository(secure)))
			.addFilterBefore(new JwtCookieFilter(jwt), UsernamePasswordAuthenticationFilter.class);

		configureAuthorization(http);
		configureLogin(http, jwt);
		configureLogout(http, jwt);
		configureExceptionHandling(http);

		return http.build();
	}

	private CookieCsrfTokenRepository csrfTokenRepository(boolean secure) {
		var repository = new CookieCsrfTokenRepository();
		repository.setCookiePath("/api/v1");
		repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(secure).sameSite("Lax"));
		return repository;
	}

	private void configureAuthorization(HttpSecurity http) {
		http.authorizeHttpRequests(authorize -> {
			if (apiDocsEnabled) {
				authorize.requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml")
					.permitAll();
			}
			if (apiDocsEnabled && swaggerUiEnabled) {
				authorize.requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**").permitAll();
			}
			authorize.dispatcherTypeMatchers(DispatcherType.ERROR)
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/accounts", LOGIN_URL)
				.permitAll()
				.requestMatchers("/api/v1/accounts/me", "/api/v1/accounts/me/preferences", "/api/v1/auth/me")
				.authenticated()
				.requestMatchers("/api/v1/customers", "/api/v1/customers/**", "/api/v1/work-orders",
						"/api/v1/work-orders/**", "/api/v1/payments/**", "/api/v1/collaborators",
						"/api/v1/collaborators/**", "/api/v1/recurrence-series", "/api/v1/recurrence-series/**",
						"/api/v1/reports/**")
				.authenticated()
				.anyRequest()
				.denyAll();
		});
	}

	private void configureLogin(HttpSecurity http, JwtCookieService jwt) {
		http.formLogin(login -> login.loginPage(LOGIN_URL)
			.loginProcessingUrl(LOGIN_URL)
			.usernameParameter("email")
			.successHandler((request, response, authentication) -> {
				var principal = (AccountPrincipal) authentication.getPrincipal();
				response.addHeader(HttpHeaders.SET_COOKIE, jwt.cookie(jwt.issue(principal)).toString());
				response.setStatus(HttpStatus.NO_CONTENT.value());
			})
			.failureHandler((request, response, exception) -> writeProblem(response, HttpStatus.UNAUTHORIZED,
					"Credenciais inválidas.")));
	}

	private void configureLogout(HttpSecurity http, JwtCookieService jwt) {
		http.logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
			.addLogoutHandler((request, response, authentication) -> response.addHeader(HttpHeaders.SET_COOKIE,
					jwt.cookie("").toString()))
			.logoutSuccessHandler(
					(request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value())));
	}

	private void configureExceptionHandling(HttpSecurity http) {
		http.exceptionHandling(errors -> errors
			.authenticationEntryPoint((request, response, exception) -> writeProblem(response, HttpStatus.UNAUTHORIZED,
					"Autenticação necessária."))
			.accessDeniedHandler(
					(request, response, exception) -> writeProblem(response, HttpStatus.FORBIDDEN, "Acesso negado.")));
	}

	private static void writeProblem(HttpServletResponse response, HttpStatus status, String title) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter()
			.write("{\"type\":\"about:blank\",\"status\":" + status.value() + ",\"title\":\"" + title + "\"}");
	}

}
