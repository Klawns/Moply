package com.klaus.moply.shared.infra.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import com.klaus.moply.shared.exception.ErrorCategory;
import com.klaus.moply.shared.exception.CoreException;

public final class ApiProblemDetails {

	private ApiProblemDetails() {
	}

	public static ProblemDetail from(HttpStatus status, CoreException exception) {
		return withCode(ProblemDetail.forStatusAndDetail(status, exception.getMessage()), exception.category(),
				exception.code());
	}

	public static ProblemDetail withCode(ProblemDetail problem, ErrorCategory category, String code) {
		problem.setProperty("category", category.name());
		problem.setProperty("code", code);
		return problem;
	}

}
