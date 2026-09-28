package com.klaus.moply.orderservice.infra.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.klaus.moply.orderservice.application.usecase.exception.OrderServiceNotFoundException;
import com.klaus.moply.orderservice.domain.exception.DomainException;

@ControllerAdvice
public class ControllerExceptionHandler {

	@ExceptionHandler(OrderServiceNotFoundException.class)
	private ResponseEntity<ProblemDetail> handleOrderServiceNotFoundException(OrderServiceNotFoundException e) {
		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problemDetail.setTitle(e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
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

	@ExceptionHandler(Exception.class)
	private ResponseEntity<ProblemDetail> handleException(Exception e) {

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problemDetail.setTitle("Ocorreu um erro interno.");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
	}

}
