package com.klaus.moply.auth.infra.web.dto;

public record CsrfResponse(String headerName, String token) {
}
