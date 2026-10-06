package com.klaus.moply.customers.infra.web.api;

import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.customers.infra.web.dto.CustomerLocationRequest;
import com.klaus.moply.customers.infra.web.dto.CustomerLocationResponse;
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

@Tag(name = "Locais dos clientes")
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
public interface CustomerLocationApi {

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Cadastrar local do cliente", operationId = "customerLocation_addLocation")
	@ApiResponse(responseCode = "201", description = "Recurso criado.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<CustomerLocationResponse> addLocation(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do cliente.") UUID customerId, CustomerLocationRequest request);

	@Operation(summary = "Listar locais do cliente", operationId = "customerLocation_locations")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	PageResponse<CustomerLocationResponse> locations(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do cliente.") UUID customerId,
			@Parameter(description = "Página, começando em zero.",
					schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")) Integer page,
			@Parameter(description = "Quantidade de itens por página.",
					schema = @Schema(type = "integer", defaultValue = "20", minimum = "1",
							maximum = "100")) Integer size,
			@Parameter(description = "Campo de ordenação.",
					schema = @Schema(allowableValues = { "name", "id" })) String sort,
			@Parameter(description = "ASC ou DESC; padrão ASC. Ao informar direction, informe também sort.",
					schema = @Schema(allowableValues = { "ASC", "DESC" })) String direction);

	@Operation(summary = "Consultar local do cliente", operationId = "customerLocation_location")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	CustomerLocationResponse location(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do cliente.") UUID customerId,
			@Parameter(description = "Identificador do local.") UUID locationId);

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Atualizar local do cliente", operationId = "customerLocation_updateLocation")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "O cliente foi alterado por outra operação.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	CustomerLocationResponse updateLocation(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do cliente.") UUID customerId,
			@Parameter(description = "Identificador do local.") UUID locationId, CustomerLocationRequest request);

}
