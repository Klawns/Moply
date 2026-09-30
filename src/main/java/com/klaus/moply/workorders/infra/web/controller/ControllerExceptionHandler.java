package com.klaus.moply.workorders.infra.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.exception.CustomerLocationNotFoundException;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;

@ControllerAdvice
public class ControllerExceptionHandler {

	@ExceptionHandler({ WorkOrderNotFoundException.class, CustomerNotFoundException.class,
			CustomerLocationNotFoundException.class,
			com.klaus.moply.collaborators.application.exception.CollaboratorNotFoundException.class })
	private ResponseEntity<ProblemDetail> handleNotFoundException(RuntimeException e) {
		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problemDetail.setTitle(e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
	}

	@ExceptionHandler({ com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException.class,
			WorkOrderStateException.class })
	public ResponseEntity<ProblemDetail> inactive(RuntimeException e) {
		return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage()));
	}

	@ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ProblemDetail> unsupported(Exception e) {
		return ResponseEntity.status(405).body(ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED));
	}

	@ExceptionHandler(DomainException.class)
	private ResponseEntity<ProblemDetail> handleDomainException(DomainException e) {
		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

		problemDetail.setTitle(e.getMessage());

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {

		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

		problem.setTitle("Erro de validação");

		problem.setDetail(exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getDefaultMessage())
			.findFirst()
			.orElse("Dados inválidos."));

		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class })
	public ResponseEntity<ProblemDetail> handleMalformedRequest(Exception exception) {
		return ResponseEntity.badRequest()
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos."));
	}

	@ExceptionHandler({ org.springframework.dao.OptimisticLockingFailureException.class,
			org.springframework.dao.PessimisticLockingFailureException.class })
	public ResponseEntity<ProblemDetail> conflict(Exception exception) {
		return ResponseEntity.status(409)
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
					"O registro foi alterado por outra operação. Recarregue e tente novamente."));
	}

	@ExceptionHandler(Exception.class)
	private ResponseEntity<ProblemDetail> handleException(Exception e) {

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problemDetail.setTitle("Ocorreu um erro interno.");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
	}

}
