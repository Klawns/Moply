package com.klaus.moply.auth.infra.web.api;

import org.springframework.http.ProblemDetail;
import org.springframework.security.web.csrf.CsrfToken;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.auth.infra.web.dto.CsrfResponse;
import com.klaus.moply.auth.infra.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autenticação")
@ApiResponses({
		@ApiResponse(responseCode = "403", description = "Acesso negado ou token CSRF inválido.",
				content = @Content(mediaType = "application/problem+json",
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "500", description = "Erro interno.",
				content = @Content(mediaType = "application/problem+json",
						schema = @Schema(implementation = ProblemDetail.class))) })
public interface AuthApi {

	@Operation(summary = "Obter token CSRF", operationId = "auth_csrf",
			description = "Use o token retornado no header informado. Obtenha um novo token após login ou logout.")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	CsrfResponse csrf(@Parameter(hidden = true) CsrfToken token);

	@Operation(summary = "Consultar usuário autenticado", operationId = "auth_me")
	@SecurityRequirement(name = "cookieAuth")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "401", description = "Autenticação necessária.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	UserResponse me(@Parameter(hidden = true) AccountPrincipal principal);

}
