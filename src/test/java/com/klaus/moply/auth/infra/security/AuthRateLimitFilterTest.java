package com.klaus.moply.auth.infra.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.ObjectMapper;
import com.klaus.moply.auth.infra.config.AuthRateLimitProperties;
import com.klaus.moply.auth.infra.security.AuthRateLimitStore.Bucket;
import jakarta.servlet.FilterChain;

class AuthRateLimitFilterTest {

	private final AuthRateLimitStore store = mock(AuthRateLimitStore.class);

	private final FilterChain chain = mock(FilterChain.class);

	private final Instant now = Instant.parse("2026-10-09T12:00:00Z");

	private final AuthRateLimitFilter filter = new AuthRateLimitFilter(store,
			new AuthRateLimitProperties(20, 3, 5, 60, 3, 1200, 10000, List.of()), Clock.fixed(now, ZoneOffset.UTC),
			new ObjectMapper());

	private MockHttpServletRequest request(String path, String ip, String email) {
		var request = new MockHttpServletRequest("POST", path);
		request.setServletPath(path);
		request.setRemoteAddr(ip);
		request.setParameter("email", email);
		return request;
	}

	@Test
	void shouldNormalizeIdentifiersIgnoreForgedHeadersAndSeparateClients() throws Exception {
		var first = request("/api/v1/auth/login", "192.0.2.1", " Owner@Example.COM ");
		first.addHeader("X-Forwarded-For", "198.51.100.1");
		first.addHeader("Forwarded", "for=198.51.100.1");
		filter.doFilter(first, new MockHttpServletResponse(), chain);
		filter.doFilter(request("/api/v1/auth/login", "192.0.2.1", "owner@example.com"), new MockHttpServletResponse(),
				chain);
		filter.doFilter(request("/api/v1/auth/login", "192.0.2.2", "other@example.com"), new MockHttpServletResponse(),
				chain);
		ArgumentCaptor<List<Bucket>> keys = ArgumentCaptor.forClass(List.class);
		verify(store, times(3)).consume(keys.capture(), eq(now), eq(10000));
		assertEquals(keys.getAllValues().get(0), keys.getAllValues().get(1));
		assertNotEquals(keys.getAllValues().get(0).getFirst().key(), keys.getAllValues().get(2).getFirst().key());
		assertNotEquals(keys.getAllValues().get(0).getLast().key(), keys.getAllValues().get(2).getLast().key());
		assertFalse(keys.getValue().toString().contains("example.com"));
	}

	@Test
	void shouldReturnProblemAndRetryAfterWithoutCallingAuthentication() throws Exception {
		when(store.consume(anyList(), eq(now), eq(10000))).thenReturn(60L);
		var response = new MockHttpServletResponse();
		filter.doFilter(request("/api/v1/auth/login", "192.0.2.1", "owner@example.com"), response, chain);
		assertEquals(429, response.getStatus());
		assertEquals("60", response.getHeader("Retry-After"));
		assertEquals("no-store", response.getHeader("Cache-Control"));
		assertTrue(response.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
		assertTrue(response.getContentAsString().contains("SECURITY_ERROR"));
		assertNotNull(response.getHeader("X-Request-ID"));
		verifyNoInteractions(chain);
	}

	@Test
	void shouldFailClosedWhenStorageIsUnavailable() throws Exception {
		when(store.consume(anyList(), any(), anyInt())).thenThrow(new DataAccessResourceFailureException("offline"));
		var response = new MockHttpServletResponse();
		filter.doFilter(request("/api/v1/accounts", "192.0.2.1", "owner@example.com"), response, chain);
		assertEquals(503, response.getStatus());
		assertTrue(response.getContentAsString().contains("AUTH_STORAGE_UNAVAILABLE"));
		verifyNoInteractions(chain);
	}

	@Test
	void shouldLeaveCsrfLogoutAndOperationalRoutesAvailable() throws Exception {
		for (String path : List.of("/api/v1/auth/csrf", "/api/v1/auth/logout", "/api/v1/customers")) {
			filter.doFilter(request(path, "192.0.2.1", ""), new MockHttpServletResponse(), chain);
		}
		verifyNoInteractions(store);
		verify(chain, times(3)).doFilter(any(), any());
	}

}
