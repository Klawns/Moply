package com.klaus.moply.auth.infra.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

	@Test
	void shouldAcceptOnlyOneLiteralFromAnExplicitlyTrustedPeer() {
		var resolver = new ClientIpResolver(List.of("192.0.2.10"));
		var request = new MockHttpServletRequest();
		request.setRemoteAddr("192.0.2.10");
		request.addHeader("X-Real-IP", "198.51.100.42");
		assertEquals("198.51.100.42", resolver.resolve(request));
		request.setRemoteAddr("192.0.2.11");
		assertEquals("192.0.2.11", resolver.resolve(request));
	}

	@Test
	void shouldIgnoreChainsHostnamesInvalidAddressesAndOtherHeaders() {
		var resolver = new ClientIpResolver(List.of("192.0.2.10"));
		for (String value : List.of("198.51.100.1, 198.51.100.2", "attacker.example", "999.1.1.1", "abcd", "")) {
			var request = new MockHttpServletRequest();
			request.setRemoteAddr("192.0.2.10");
			request.addHeader("X-Real-IP", value);
			request.addHeader("X-Forwarded-For", "198.51.100.1");
			assertEquals("192.0.2.10", resolver.resolve(request));
		}
		assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver(List.of("proxy.example")));
	}

	@Test
	void shouldCanonicalizeIpv6SoItCannotCreateMultipleBudgets() {
		var resolver = new ClientIpResolver(List.of());
		var first = new MockHttpServletRequest();
		first.setRemoteAddr("2001:db8::1");
		var other = new MockHttpServletRequest();
		other.setRemoteAddr("2001:0db8:0:0:0:0:0:1");
		assertEquals(resolver.resolve(first), resolver.resolve(other));
	}

}
