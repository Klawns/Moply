package com.klaus.moply.recurrence.infra.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import com.klaus.moply.recurrence.application.usecase.exception.SeriesNotFoundException;

@RestControllerAdvice
public class RecurrenceExceptionHandler {

	@ExceptionHandler(SeriesNotFoundException.class)
	public ResponseEntity<ProblemDetail> notFound(SeriesNotFoundException exception) {
		return ResponseEntity.status(404)
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage()));
	}

}
