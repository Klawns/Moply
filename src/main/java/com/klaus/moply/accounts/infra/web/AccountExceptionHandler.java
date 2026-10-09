package com.klaus.moply.accounts.infra.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.klaus.moply.accounts.application.usecase.exception.AccountConflictException;
import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.shared.infra.web.ApiProblemDetails;

@RestControllerAdvice(assignableTypes = { AccountController.class, AccountPreferencesController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccountExceptionHandler {

	@ExceptionHandler(AccountConflictException.class)
	public ProblemDetail conflict(AccountConflictException exception) {
		return ApiProblemDetails.from(HttpStatus.CONFLICT, exception);
	}

	@ExceptionHandler(AccountNotFoundException.class)
	public ProblemDetail notFound(AccountNotFoundException exception) {
		return ApiProblemDetails.from(HttpStatus.NOT_FOUND, exception);
	}

}
