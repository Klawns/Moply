package com.klaus.moply.payments.infra.web.api;

import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.payments.infra.web.dto.PaymentResponse;
import com.klaus.moply.payments.infra.web.dto.RecordCollaboratorPaymentRequest;
import com.klaus.moply.payments.infra.web.dto.ReverseCollaboratorPaymentRequest;
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

@Tag(name = "Pagamentos aos colaboradores")
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
public interface PaymentWorkOrderCollaboratorApi {

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Registrar pagamento ao colaborador", operationId = "paymentWorkOrderCollaborator_record")
	@ApiResponse(responseCode = "201", description = "Recurso criado.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Conflito com o estado atual do recurso.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<PaymentResponse> record(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do trabalho.") UUID workOrderId,
			@Parameter(description = "Identificador do colaborador.") UUID collaboratorId,
			@Parameter(description = "Chave para evitar pagamentos duplicados.") String idempotencyKey,
			RecordCollaboratorPaymentRequest request);

	@Operation(summary = "Listar pagamentos ao colaborador", operationId = "paymentWorkOrderCollaborator_list")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	PageResponse<PaymentResponse> list(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do trabalho.") UUID workOrderId,
			@Parameter(description = "Identificador do colaborador.") UUID collaboratorId,
			@Parameter(description = "Página, começando em zero.",
					schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")) Integer page,
			@Parameter(description = "Quantidade de itens por página.",
					schema = @Schema(type = "integer", defaultValue = "20", minimum = "1",
							maximum = "100")) Integer size,
			@Parameter(description = "Campo de ordenação.",
					schema = @Schema(
							allowableValues = { "recordedAt", "paidOn", "amount", "status", "id" })) String sort,
			@Parameter(description = "ASC ou DESC; padrão ASC. Ao informar direction, informe também sort.",
					schema = @Schema(allowableValues = { "ASC", "DESC" })) String direction);

	@SecurityRequirements({ @SecurityRequirement(name = "cookieAuth"), @SecurityRequirement(name = "csrfToken") })
	@Operation(summary = "Reverter pagamento ao colaborador", operationId = "paymentWorkOrderCollaborator_reverse")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Recurso não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Conflito com o estado atual do recurso.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	PaymentResponse reverse(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Identificador do trabalho.") UUID workOrderId,
			@Parameter(description = "Identificador do colaborador.") UUID collaboratorId,
			@Parameter(description = "Identificador do pagamento.") UUID paymentId,
			ReverseCollaboratorPaymentRequest request);

}
