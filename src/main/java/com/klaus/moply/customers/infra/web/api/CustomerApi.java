package com.klaus.moply.customers.infra.web.api;

import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.customers.infra.web.dto.CreateCustomerRequest;
import com.klaus.moply.customers.infra.web.dto.CustomerResponse;
import com.klaus.moply.customers.infra.web.dto.UpdateCustomerRequest;
import com.klaus.moply.shared.infra.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Clientes")
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
public interface CustomerApi {

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Cadastrar cliente", operationId = "customer_create")
	@ApiResponse(responseCode = "201", description = "Recurso criado.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Conflito com o estado atual do recurso.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<UUID> create(@Parameter(hidden = true) AccountPrincipal principal, CreateCustomerRequest request);

	@Operation(summary = "Listar clientes", operationId = "customer_list")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	PageResponse<CustomerResponse> list(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Página, começando em zero.",
					schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")) Integer page,
			@Parameter(description = "Quantidade de itens por página.",
					schema = @Schema(type = "integer", defaultValue = "20", minimum = "1",
							maximum = "100")) Integer size,
			@Parameter(description = "Campo de ordenação.",
					schema = @Schema(allowableValues = { "name", "id" })) String sort,
			@Parameter(description = "ASC ou DESC; padrão ASC. Ao informar direction, informe também sort.",
					schema = @Schema(allowableValues = { "ASC", "DESC" })) String direction);

	@Operation(summary = "Consultar cliente", operationId = "customer_find")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	CustomerResponse find(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do cliente.") UUID customerId);

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Atualizar cliente", operationId = "customer_update")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Conflito com o estado atual do recurso.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	CustomerResponse update(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do cliente.") UUID customerId, UpdateCustomerRequest request);

}
