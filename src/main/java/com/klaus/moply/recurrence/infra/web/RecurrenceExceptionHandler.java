package com.klaus.moply.recurrence.infra.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import com.klaus.moply.recurrence.application.usecase.exception.SeriesNotFoundException;
import com.klaus.moply.shared.infra.web.ApiProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RecurrenceExceptionHandler {

	@ExceptionHandler(SeriesNotFoundException.class)
	public ResponseEntity<ProblemDetail> notFound(SeriesNotFoundException exception) {
		return ResponseEntity.status(404).body(ApiProblemDetails.from(HttpStatus.NOT_FOUND, exception));
	}

}
