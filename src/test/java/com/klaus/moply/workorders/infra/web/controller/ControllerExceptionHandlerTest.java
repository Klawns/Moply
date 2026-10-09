package com.klaus.moply.workorders.infra.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class ControllerExceptionHandlerTest {

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
			org.springframework.test.util.JsonPathExpectationsHelper jsonPath = new org.springframework.test.util.JsonPathExpectationsHelper(
					"$.requestId");
			jsonPath.assertValue(response.getContentAsString(), requestId);
			var event = appender.list.getFirst();
			assertEquals(Level.ERROR, event.getLevel());
			assertEquals("Erro interno na API. requestId=" + requestId, event.getFormattedMessage());
			assertNotNull(event.getThrowableProxy());
			assertEquals("Unexpected database failure", event.getThrowableProxy().getMessage());
		}
		finally {
			logger.detachAppender(appender);
			appender.stop();
		}
	}

	@RestController
	static class FailingController {

		@GetMapping("/failure")
		public void fail() {
			throw new IllegalStateException("Unexpected database failure");
		}

	}

}
