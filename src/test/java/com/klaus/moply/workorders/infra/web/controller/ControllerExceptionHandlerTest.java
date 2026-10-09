package com.klaus.moply.workorders.infra.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.shared.domain.exception.DomainException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class ControllerExceptionHandlerTest {

	@Test
	void shouldExposeStableCategoryAndCodeForDomainAndApplicationErrors() throws Exception {
		var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
			.setControllerAdvice(new ControllerExceptionHandler())
			.build();

		mvc.perform(get("/domain-failure"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.category").value("DOMAIN_ERROR"))
			.andExpect(jsonPath("$.code").value("DOMAIN_ERROR"))
			.andExpect(jsonPath("$.title").value("Invalid domain state"));

		mvc.perform(get("/application-failure"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.category").value("APPLICATION_ERROR"))
			.andExpect(jsonPath("$.code").value("APPLICATION_ERROR"))
			.andExpect(jsonPath("$.title").value("Invalid application input"));
	}

	@Test
	void shouldLogInternalErrorAndReturnMatchingRequestIdWithoutExceptionDetails() throws Exception {
		var logger = (Logger) LoggerFactory.getLogger(ControllerExceptionHandler.class);
		var appender = new ListAppender<ILoggingEvent>();
		appender.start();
		logger.addAppender(appender);
		try {
			var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
				.setControllerAdvice(new ControllerExceptionHandler())
				.build();
			var response = mvc.perform(get("/failure")).andReturn().getResponse();
			assertEquals(500, response.getStatus());
			var requestId = response.getHeader("X-Request-ID");
			assertNotNull(requestId);
			UUID.fromString(requestId);
			assertFalse(response.getContentAsString().contains("Unexpected database failure"));
			assertFalse(response.getContentAsString().contains("java.lang.IllegalStateException"));
			org.springframework.test.util.JsonPathExpectationsHelper jsonPath = new org.springframework.test.util.JsonPathExpectationsHelper(
					"$.requestId");
			jsonPath.assertValue(response.getContentAsString(), requestId);
			new org.springframework.test.util.JsonPathExpectationsHelper("$.category")
				.assertValue(response.getContentAsString(), "INTERNAL_ERROR");
			new org.springframework.test.util.JsonPathExpectationsHelper("$.code")
				.assertValue(response.getContentAsString(), "INTERNAL_ERROR");
			var event = appender.list.getFirst();
			assertEquals(Level.ERROR, event.getLevel());
			assertEquals("Unexpected API request failure", event.getFormattedMessage());
			assertStructuredField(event, "event", "api.request.failed");
			assertStructuredField(event, "requestId", requestId);
			assertStructuredField(event, "category", "INTERNAL_ERROR");
			assertStructuredField(event, "code", "INTERNAL_ERROR");
			assertStructuredField(event, "method", "GET");
			assertStructuredField(event, "path", "/failure");
			assertStructuredField(event, "exceptionType", IllegalStateException.class.getName());
			assertNotNull(event.getThrowableProxy());
			assertEquals("Unexpected database failure", event.getThrowableProxy().getMessage());
		}
		finally {
			logger.detachAppender(appender);
			appender.stop();
		}
	}

	private void assertStructuredField(ILoggingEvent event, String key, String value) {
		assertTrue(event.getKeyValuePairs().stream().anyMatch(pair -> pair.key.equals(key) && pair.value.equals(value)),
				"Missing log field " + key);
	}

	@RestController
	static class FailingController {

		@GetMapping("/domain-failure")
		public void domainFailure() {
			throw new DomainException("Invalid domain state");
		}

		@GetMapping("/application-failure")
		public void applicationFailure() {
			throw new ApplicationException("Invalid application input");
		}

		@GetMapping("/failure")
		public void fail() {
			throw new IllegalStateException("Unexpected database failure");
		}

	}

}
