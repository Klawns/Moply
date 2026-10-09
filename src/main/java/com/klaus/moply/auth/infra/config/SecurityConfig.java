package com.klaus.moply.auth.infra.config;

import java.time.Clock;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.web.csrf.CsrfFilter;
import com.klaus.moply.auth.infra.security.AuthRateLimitFilter;
import com.klaus.moply.auth.infra.security.AuthRateLimitStore;
import com.klaus.moply.auth.infra.security.SecurityProblemWriter;
import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

@Configuration
@EnableConfigurationProperties(AuthRateLimitProperties.class)
@org.springframework.scheduling.annotation.EnableScheduling
public class SecurityConfig {

	@Value("${springdoc.api-docs.enabled:false}")
	private boolean apiDocsEnabled;

	@Value("${springdoc.swagger-ui.enabled:false}")
	private boolean swaggerUiEnabled;

	private static final String LOGIN_URL = "/api/v1/auth/login";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtCookieService jwt,
			@Value("${moply.auth.cookie.secure:false}") boolean secure, ObjectMapper objectMapper,
			AuthRateLimitStore rateLimits, AuthRateLimitProperties ratePolicy, Clock clock) throws Exception {
		http.headers(headers -> headers.referrerPolicy(referrer -> referrer.policy(
				org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)));
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
			.requestCache(AbstractHttpConfigurer::disable)
			.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository(secure)))
			.addFilterAfter(new AuthRateLimitFilter(rateLimits, ratePolicy, clock, objectMapper), CsrfFilter.class)
			.addFilterBefore(new JwtCookieFilter(jwt, objectMapper), UsernamePasswordAuthenticationFilter.class);

		configureAuthorization(http);
		configureLogin(http, jwt, objectMapper);
		configureLogout(http, jwt, objectMapper);
		configureExceptionHandling(http, objectMapper);

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
				.requestMatchers("/api/v1/locations", "/api/v1/customers", "/api/v1/customers/**",
						"/api/v1/work-orders", "/api/v1/work-orders/**", "/api/v1/payments/**", "/api/v1/collaborators",
						"/api/v1/collaborators/**", "/api/v1/recurrence-series", "/api/v1/recurrence-series/**",
						"/api/v1/reports/**")
				.authenticated()
				.anyRequest()
				.denyAll();
		});
	}

	private void configureLogin(HttpSecurity http, JwtCookieService jwt, ObjectMapper objectMapper) {
		http.formLogin(login -> login.loginPage(LOGIN_URL)
			.loginProcessingUrl(LOGIN_URL)
			.usernameParameter("email")
			.successHandler((request, response, authentication) -> {
				var principal = (AccountPrincipal) authentication.getPrincipal();
				response.addHeader(HttpHeaders.SET_COOKIE, jwt.cookie(jwt.issue(principal)).toString());
				response.setStatus(HttpStatus.NO_CONTENT.value());
			})
			.failureHandler((request, response, exception) -> SecurityProblemWriter.write(response,
					HttpStatus.UNAUTHORIZED, "Credenciais inválidas.", "INVALID_CREDENTIALS", objectMapper)));
	}

	private void configureLogout(HttpSecurity http, JwtCookieService jwt, ObjectMapper mapper) {
		http.logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
			.logoutSuccessHandler((request, response, authentication) -> {
				try {
					String token = null;
					boolean duplicate = false;
					if (request.getCookies() != null)
						for (var cookie : request.getCookies()) {
							if (JwtCookieService.COOKIE.equals(cookie.getName())) {
								if (token != null)
									duplicate = true;
								token = cookie.getValue();
							}
						}
					if (duplicate) {
						SecurityProblemWriter.write(response, HttpStatus.BAD_REQUEST, "Cookie de autenticação ambíguo.",
								"INVALID_AUTH_COOKIE", mapper);
						return;
					}
					if (token != null)
						jwt.revoke(token);
					response.addHeader(HttpHeaders.SET_COOKIE, jwt.cookie("").toString());
					response.setStatus(HttpStatus.NO_CONTENT.value());
				}
				catch (DataAccessException | org.springframework.transaction.TransactionException exception) {
					SecurityProblemWriter.write(response, HttpStatus.SERVICE_UNAVAILABLE,
							"Não foi possível encerrar a sessão. Tente novamente.", "AUTH_STORAGE_UNAVAILABLE", mapper);
				}
			}));
	}

	private void configureExceptionHandling(HttpSecurity http, ObjectMapper objectMapper) {
		http.exceptionHandling(errors -> errors
			.authenticationEntryPoint((request, response, exception) -> SecurityProblemWriter.write(response,
					HttpStatus.UNAUTHORIZED, "Autenticação necessária.", "AUTHENTICATION_REQUIRED", objectMapper))
			.accessDeniedHandler((request, response, exception) -> SecurityProblemWriter.write(response,
					HttpStatus.FORBIDDEN, "Acesso negado.", "ACCESS_DENIED", objectMapper)));
	}

}
