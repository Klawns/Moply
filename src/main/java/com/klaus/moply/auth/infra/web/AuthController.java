package com.klaus.moply.auth.infra.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.auth.infra.web.api.AuthApi;
import com.klaus.moply.auth.infra.web.dto.CsrfResponse;
import com.klaus.moply.auth.infra.web.dto.UserResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApi {

	@GetMapping("/csrf")
	@Override
	public CsrfResponse csrf(CsrfToken token) {
		return new CsrfResponse(token.getHeaderName(), token.getToken());
	}

	@GetMapping("/me")
	@Override
	public UserResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
		return new UserResponse(principal.getUserId(), principal.getOrganizationId(), principal.getUsername());
	}

}
