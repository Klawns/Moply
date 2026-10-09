package com.klaus.moply.auth.infra.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

/** Trust exact proxy addresses only, never a client-supplied forwarding chain. */
public final class ClientIpResolver {

	private final Set<String> trusted;

	public ClientIpResolver(List<String> proxies) {
		trusted = proxies == null ? Set.of() : proxies.stream().filter(value -> !value.isBlank()).map(value -> {
			String address = literal(value.strip());
			if (address == null)
				throw new IllegalArgumentException("Trusted proxies must be literal IP addresses");
			return address;
		}).collect(Collectors.toUnmodifiableSet());
	}

	public String resolve(HttpServletRequest request) {
		String peer = literal(request.getRemoteAddr());
		if (peer == null)
			return request.getRemoteAddr();
		if (!trusted.contains(peer))
			return peer;
		String supplied = literal(request.getHeader("X-Real-IP"));
		return supplied == null ? peer : supplied;
	}

	private static String literal(String value) {
		if (value == null || value.length() > 45)
			return null;
		// Ensure no DNS lookup can be induced by a request header.
		if (!(value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")
				|| (value.contains(":") && value.matches("[0-9a-fA-F:.]+"))))
			return null;
		try {
			return InetAddress.getByName(value).getHostAddress();
		}
		catch (UnknownHostException exception) {
			return null;
		}
	}

}
