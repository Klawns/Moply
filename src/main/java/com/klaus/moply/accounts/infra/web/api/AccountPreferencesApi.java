package com.klaus.moply.accounts.infra.web.api;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.klaus.moply.accounts.infra.web.dto.PreferencesRequest;
import com.klaus.moply.accounts.infra.web.dto.PreferencesResponse;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Preferências da conta")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Dados inválidos.",
				content = @Content(mediaType = "application/problem+json",
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "401", description = "Autenticação necessária.",
				content = @Content(mediaType = "application/problem+json",
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "403", description = "Acesso negado ou token CSRF inválido.",
				content = @Content(mediaType = "application/problem+json",
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "500", description = "Erro interno.",
				content = @Content(mediaType = "application/problem+json",
						schema = @Schema(implementation = ProblemDetail.class))) })
public interface AccountPreferencesApi {

	@Operation(summary = "Consultar preferências da conta", operationId = "accountPreferences_preferences")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Conta não encontrada.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	PreferencesResponse preferences(@Parameter(hidden = true) AccountPrincipal principal);

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Atualizar preferências da conta", operationId = "accountPreferences_update")
	@ApiResponse(responseCode = "204", description = "Operação concluída.", content = @Content)
	@ApiResponse(responseCode = "409", description = "Conflito com o estado atual do recurso.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Conta não encontrada.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<Void> update(@Parameter(hidden = true) AccountPrincipal principal, PreferencesRequest request);

}
