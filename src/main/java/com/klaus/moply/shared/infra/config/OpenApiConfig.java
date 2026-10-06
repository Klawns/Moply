package com.klaus.moply.shared.infra.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import com.klaus.moply.auth.infra.security.JwtCookieService;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

	@Bean
	OpenAPI moplyOpenApi() {
		return new OpenAPI()
			.info(new Info().title("Moply API")
				.version("v1")
				.description("Gestão de equipes, trabalhos e pagamentos. "
						+ "Obtenha o token em /api/v1/auth/csrf e informe-o em Authorize (csrfToken). "
						+ "Faça login com email e password; o navegador envia o cookie automaticamente. "
						+ "Após login ou logout, obtenha um novo token CSRF e atualize Authorize."))
			.components(new Components()
				.addSecuritySchemes("cookieAuth", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
					.in(SecurityScheme.In.COOKIE)
					.name(JwtCookieService.COOKIE)
					.description("Cookie HttpOnly emitido pelo login e enviado automaticamente pelo navegador."))
				.addSecuritySchemes("csrfToken", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
					.in(SecurityScheme.In.HEADER)
					.name("X-XSRF-TOKEN")
					.description("Copie o campo token de /api/v1/auth/csrf. Necessário nas operações de escrita.")));
	}

	@Bean
	OpenApiCustomizer securityFilterOperations() {
		return api -> {
			// Annotation requirements are alternatives; writes require cookie AND CSRF.
			api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
				var requirements = operation.getSecurity();
				if (requirements != null
						&& requirements.stream().anyMatch(requirement -> requirement.containsKey("cookieAuth"))
						&& requirements.stream().anyMatch(requirement -> requirement.containsKey("csrfToken"))) {
					operation
						.setSecurity(List.of(new SecurityRequirement().addList("cookieAuth").addList("csrfToken")));
				}
			}));
			var credentials = new ObjectSchema().addProperty("email", new StringSchema().format("email"))
				.addProperty("password", new StringSchema().format("password"));
			credentials.setRequired(List.of("email", "password"));
			var login = securityOperation("auth_login", "Entrar na conta").description(
					"Autentica por formulário e emite o cookie MOPLY_AUTH. Obtenha um novo token CSRF após o login.")
				.requestBody(new RequestBody().required(true)
					.content(new Content().addMediaType(MediaType.APPLICATION_FORM_URLENCODED_VALUE,
							new io.swagger.v3.oas.models.media.MediaType().schema(credentials))))
				.responses(new ApiResponses()
					.addApiResponse("204", new ApiResponse().description("Autenticado; cookie emitido."))
					.addApiResponse("401", problemResponse("Credenciais inválidas."))
					.addApiResponse("403", problemResponse("Token CSRF inválido.")));
			var logout = securityOperation("auth_logout", "Sair da conta")
				.description("Remove o cookie de autenticação. Obtenha um novo token CSRF após o logout.")
				.responses(new ApiResponses()
					.addApiResponse("204", new ApiResponse().description("Cookie de autenticação removido."))
					.addApiResponse("403", problemResponse("Token CSRF inválido.")));
			api.path("/api/v1/auth/login", new PathItem().post(login));
			api.path("/api/v1/auth/logout", new PathItem().post(logout));
		};
	}

	private Operation securityOperation(String id, String summary) {
		return new Operation().operationId(id)
			.summary(summary)
			.tags(List.of("Autenticação"))
			.security(List.of(new SecurityRequirement().addList("csrfToken")));
	}

	private ApiResponse problemResponse(String description) {
		return new ApiResponse().description(description)
			.content(new Content().addMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					new io.swagger.v3.oas.models.media.MediaType()
						.schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
	}

}
