package com.klaus.moply.collaborators.infra.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.shared.exception.ErrorCategory;
import com.klaus.moply.shared.infra.web.ApiProblemDetails;

@RestControllerAdvice(assignableTypes = CollaboratorController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CollaboratorExceptionHandler {

	@ExceptionHandler(CollaboratorNotFoundException.class)
	public ProblemDetail notFound(CollaboratorNotFoundException exception) {
		return ApiProblemDetails.from(HttpStatus.NOT_FOUND, exception);
	}

	@ExceptionHandler(InactiveCollaboratorException.class)
	public ProblemDetail inactive(InactiveCollaboratorException exception) {
		return ApiProblemDetails.from(HttpStatus.CONFLICT, exception);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail concurrentUpdate(OptimisticLockingFailureException exception) {
		return ApiProblemDetails.withCode(
				ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
						"O colaborador foi alterado por outra operação. Recarregue e tente novamente."),
				ErrorCategory.APPLICATION_ERROR, "CONCURRENT_UPDATE");
	}

}
