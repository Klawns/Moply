package com.klaus.moply.customers.infra.web.api;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.customers.infra.web.dto.LocationSummaryResponse;
import com.klaus.moply.shared.infra.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Locais")
@SecurityRequirement(name = "cookieAuth")
public interface LocationApi {

	@Operation(summary = "Listar locais da organização", operationId = "location_list")
	PageResponse<LocationSummaryResponse> list(@Parameter(hidden = true) AccountPrincipal principal,
			@Parameter(description = "Busca por nome, endereço ou cliente.") String q,
			@Parameter(description = "Página, começando em zero.") Integer page,
			@Parameter(description = "Itens por página, de 1 a 100; padrão 20.") Integer size,
			@Parameter(description = "name ou id; padrão name.") String sort,
			@Parameter(description = "ASC ou DESC; padrão ASC.") String direction);

}
