package com.klaus.moply.auth.infra.security;

import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;
import com.klaus.moply.shared.exception.ErrorCategory;
import com.klaus.moply.shared.infra.web.ApiProblemDetails;
import jakarta.servlet.http.HttpServletResponse;

public final class SecurityProblemWriter {

	private SecurityProblemWriter() {
	}

	public static void write(HttpServletResponse response, HttpStatus status, String title, String code,
			ObjectMapper mapper) throws IOException {
		String requestId = UUID.randomUUID().toString();
		ProblemDetail problem = ProblemDetail.forStatus(status);
		problem.setTitle(title);
		ApiProblemDetails.withCode(problem, ErrorCategory.SECURITY_ERROR, code);
		problem.setProperty("requestId", requestId);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setHeader("X-Request-ID", requestId);
		response.setHeader("Cache-Control", "no-store");
		mapper.writeValue(response.getOutputStream(), problem);
	}

}
