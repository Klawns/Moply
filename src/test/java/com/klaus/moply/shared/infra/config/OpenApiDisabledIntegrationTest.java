package com.klaus.moply.shared.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDisabledIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	RequestMappingHandlerMapping mappings;

	@Autowired
	ApplicationContext context;

	@Test
	void shouldNotRegisterOrExposeDocumentationByDefault() throws Exception {
		assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
		assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
		assertThat(context.getBeansOfType(OpenApiConfig.class)).isEmpty();
		assertThat(
				mappings.getHandlerMethods().keySet().stream().flatMap(mapping -> mapping.getPatternValues().stream()))
			.noneMatch(path -> path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui"));
		for (String path : new String[] { "/v3/api-docs", "/swagger-ui.html", "/swagger-ui/index.html" }) {
			mvc.perform(get(path)).andExpect(status().isUnauthorized());
			mvc.perform(get(path).with(user("manager"))).andExpect(status().isForbidden());
		}
	}

}
