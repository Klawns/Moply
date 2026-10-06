package com.klaus.moply.workorders.infra.web.api;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.shared.infra.web.dto.PageResponse;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.infra.web.dto.request.CancelRequest;
import com.klaus.moply.workorders.infra.web.dto.request.CreateWorkOrderRequest;
import com.klaus.moply.workorders.infra.web.dto.request.RescheduleRequest;
import com.klaus.moply.workorders.infra.web.dto.response.PricingPreviewResponse;
import com.klaus.moply.workorders.infra.web.dto.response.PricingProblemResponse;
import com.klaus.moply.workorders.infra.web.dto.response.WorkOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Trabalhos")
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
public interface WorkOrderApi {

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Simular preço e divisão da remuneração", operationId = "workOrder_preview")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	PricingPreviewResponse preview(@Parameter(hidden = true) AccountPrincipal principal, CreateWorkOrderRequest input);

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Concluir trabalho", operationId = "workOrder_complete")
	@ApiResponse(responseCode = "204", description = "Operação concluída.", content = @Content)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Conflito com o estado atual do recurso.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<Void> complete(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do recurso.") UUID id);

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Reagendar trabalho", operationId = "workOrder_reschedule")
	@ApiResponse(responseCode = "204", description = "Operação concluída.", content = @Content)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Conflito com o estado atual do recurso.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<Void> reschedule(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do recurso.") UUID id, RescheduleRequest request);

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Cancelar trabalho", operationId = "workOrder_cancel",
			description = "O corpo é opcional. O escopo e as confirmações financeiras seguem as regras de cancelamento.")
	@ApiResponse(responseCode = "204", description = "Operação concluída.", content = @Content)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Conflito com o estado atual do recurso.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<Void> cancel(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do recurso.") UUID id, CancelRequest request);

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Cadastrar trabalho", operationId = "workOrder_create")
	@ApiResponse(responseCode = "201", description = "Recurso criado.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409",
			description = "Confirmação de preços necessária (PRICING_ACCEPTANCE_REQUIRED), ou conflito com o estado atual. O erro de preços inclui pricingPreview.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(anyOf = { ProblemDetail.class, PricingProblemResponse.class })))
	@ApiResponse(responseCode = "422",
			description = "Bases individuais excedem o total (PRICING_BASES_EXCEED_TOTAL). Inclui code e pricingPreview.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = PricingProblemResponse.class)))
	ResponseEntity<WorkOrderResponse> create(@Parameter(hidden = true) AccountPrincipal principal,
			CreateWorkOrderRequest input);

	@Operation(summary = "Consultar trabalho", operationId = "workOrder_get")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	WorkOrderResponse get(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do recurso.") UUID id);

	@Operation(summary = "Listar trabalhos", operationId = "workOrder_list")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	PageResponse<WorkOrderResponse> list(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Data inicial (inclusive).") LocalDate from,
			@Parameter(description = "Data final (inclusive).") LocalDate to,
			@Parameter(description = "Identificador do cliente.") UUID customerId,
			@Parameter(description = "Estado do trabalho.") WorkOrderStatus status,
			@Parameter(description = "Página, começando em zero.",
					schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")) Integer page,
			@Parameter(description = "Quantidade de itens por página.",
					schema = @Schema(type = "integer", defaultValue = "20", minimum = "1",
							maximum = "100")) Integer size,
			@Parameter(description = "Campo de ordenação.",
					schema = @Schema(allowableValues = { "serviceDate", "startTime", "status", "id" })) String sort,
			@Parameter(description = "ASC ou DESC; padrão ASC. Ao informar direction, informe também sort.",
					schema = @Schema(allowableValues = { "ASC", "DESC" })) String direction);

}
