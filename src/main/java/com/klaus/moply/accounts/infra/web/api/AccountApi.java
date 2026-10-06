package com.klaus.moply.accounts.infra.web.api;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.klaus.moply.accounts.infra.web.dto.RegistrationRequest;
import com.klaus.moply.accounts.infra.web.dto.RegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Contas")
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Dados inválidos.",
				content = @Content(mediaType = "application/problem+json",
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "403", description = "Acesso negado ou token CSRF inválido.",
				content = @Content(mediaType = "application/problem+json",
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "500", description = "Erro interno.",
				content = @Content(mediaType = "application/problem+json",
						schema = @Schema(implementation = ProblemDetail.class))) })
public interface AccountApi {

	@SecurityRequirement(name = "csrfToken")
	@Operation(summary = "Cadastrar conta", operationId = "account_register",
			description = "Informe um fuso IANA válido. A senha deve ter pelo menos 12 caracteres e no máximo 72 bytes em UTF-8.")
	@ApiResponse(responseCode = "201", description = "Recurso criado.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "409", description = "Conta ou usuário já cadastrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<RegistrationResponse> register(RegistrationRequest request);

}
