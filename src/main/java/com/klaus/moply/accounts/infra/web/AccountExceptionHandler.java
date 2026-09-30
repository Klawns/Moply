package com.klaus.moply.accounts.infra.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.klaus.moply.accounts.application.exception.AccountConflictException;
import com.klaus.moply.accounts.application.exception.AccountNotFoundException;

@RestControllerAdvice(assignableTypes = { AccountController.class, AccountPreferencesController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccountExceptionHandler {

	@ExceptionHandler(AccountConflictException.class)
	public ProblemDetail conflict(AccountConflictException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(AccountNotFoundException.class)
	public ProblemDetail notFound(AccountNotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

}
