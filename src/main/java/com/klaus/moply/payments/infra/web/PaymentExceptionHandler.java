package com.klaus.moply.payments.infra.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.application.usecase.exception.PaymentNotFoundException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler {

	@ExceptionHandler(PaymentNotFoundException.class)
	public ResponseEntity<ProblemDetail> notFound(PaymentNotFoundException exception) {
		var problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle(exception.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(PaymentConflictException.class)
	public ResponseEntity<ProblemDetail> conflict(PaymentConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage()));
	}

}
