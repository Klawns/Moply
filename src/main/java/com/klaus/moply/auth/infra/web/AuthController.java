package com.klaus.moply.auth.infra.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	@GetMapping("/csrf")
	public CsrfResponse csrf(CsrfToken token) {
		return new CsrfResponse(token.getHeaderName(), token.getToken());
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
		return new UserResponse(principal.getUserId(), principal.getOrganizationId(), principal.getUsername());
	}

	public record CsrfResponse(String headerName, String token) {
	}

	public record UserResponse(UUID userId, UUID organizationId, String email) {
	}

}
