package com.klaus.moply.reports.infra.web.api;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.reports.infra.web.dto.request.CollaboratorReportPeriodRequest;
import com.klaus.moply.reports.infra.web.dto.response.CollaboratorsReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Relatório de colaboradores")
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
public interface CollaboratorsReportApi {

	@Operation(summary = "Consultar relatório de colaboradores", operationId = "collaboratorsReport_get")
	@ApiResponse(responseCode = "200", description = "Operação concluída.", useReturnTypeSchema = true)
	@ApiResponse(responseCode = "404", description = "Cliente ou colaborador informado não encontrado.",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	@Parameters({
			@Parameter(name = "from", in = ParameterIn.QUERY, required = true,
					description = "Data inicial (inclusive).", schema = @Schema(type = "string", format = "date")),
			@Parameter(name = "to", in = ParameterIn.QUERY, required = true, description = "Data final (inclusive).",
					schema = @Schema(type = "string", format = "date")),
			@Parameter(name = "customerId", in = ParameterIn.QUERY, description = "Identificador do cliente."),
			@Parameter(name = "collaboratorId", in = ParameterIn.QUERY, description = "Identificador do colaborador."),
			@Parameter(name = "page", in = ParameterIn.QUERY, description = "Página, começando em zero.",
					schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
			@Parameter(name = "size", in = ParameterIn.QUERY, description = "Quantidade de itens por página.",
					schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100")),
			@Parameter(name = "sort", in = ParameterIn.QUERY,
					description = "Ordena participações e acertos. Apenas id é aceito nas duas listas; omita para usar a ordem padrão de cada lista.",
					schema = @Schema(allowableValues = { "id" })),
			@Parameter(name = "direction", in = ParameterIn.QUERY,
					description = "ASC ou DESC; padrão ASC. Ao informar direction, informe também sort.",
					schema = @Schema(allowableValues = { "ASC", "DESC" })) })
	CollaboratorsReportResponse get(@Parameter(hidden = true) AccountPrincipal principal,
			@ParameterObject CollaboratorReportPeriodRequest request);

}
