package com.klaus.moply.workorders.infra.web.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.exception.CustomerLocationNotFoundException;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.exception.ErrorCategory;
import com.klaus.moply.shared.exception.CoreException;
import com.klaus.moply.shared.infra.web.ApiProblemDetails;
import com.klaus.moply.workorders.application.usecase.exception.PricingAcceptanceException;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;
import com.klaus.moply.workorders.infra.web.dto.response.PricingPreviewResponse;

@Slf4j
@ControllerAdvice
public class ControllerExceptionHandler {

	@ExceptionHandler({ WorkOrderNotFoundException.class, CustomerNotFoundException.class,
			CustomerLocationNotFoundException.class, CollaboratorNotFoundException.class })
	private ResponseEntity<ProblemDetail> handleNotFoundException(CoreException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle(exception.getMessage());
		ApiProblemDetails.withCode(problem, exception.category(), exception.code());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler({ InactiveCollaboratorException.class, WorkOrderStateException.class })
	public ResponseEntity<ProblemDetail> inactive(CoreException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiProblemDetails.from(HttpStatus.CONFLICT, exception));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ProblemDetail> unsupported(HttpRequestMethodNotSupportedException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
			.body(ApiProblemDetails.withCode(problem, ErrorCategory.APPLICATION_ERROR, "METHOD_NOT_ALLOWED"));
	}

	@ExceptionHandler(DomainException.class)
	private ResponseEntity<ProblemDetail> handleDomainException(DomainException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle(exception.getMessage());
		ApiProblemDetails.withCode(problem, exception.category(), exception.code());
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(ApplicationException.class)
	private ResponseEntity<ProblemDetail> handleApplicationException(ApplicationException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle(exception.getMessage());
		ApiProblemDetails.withCode(problem, exception.category(), exception.code());
		return ResponseEntity.badRequest().body(problem);
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
		ApiProblemDetails.withCode(problem, ErrorCategory.APPLICATION_ERROR, "VALIDATION_ERROR");
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class })
	public ResponseEntity<ProblemDetail> handleMalformedRequest(Exception exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos.");
		return ResponseEntity.badRequest()
			.body(ApiProblemDetails.withCode(problem, ErrorCategory.APPLICATION_ERROR, "MALFORMED_REQUEST"));
	}

	@ExceptionHandler({ OptimisticLockingFailureException.class, PessimisticLockingFailureException.class })
	public ResponseEntity<ProblemDetail> conflict(Exception exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"O registro foi alterado por outra operação. Recarregue e tente novamente.");
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ApiProblemDetails.withCode(problem, ErrorCategory.APPLICATION_ERROR, "CONCURRENT_UPDATE"));
	}

	@ExceptionHandler(PricingAcceptanceException.class)
	public ResponseEntity<ProblemDetail> pricing(PricingAcceptanceException exception) {
		HttpStatus status = exception.preview().canCreate() ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_CONTENT;
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
		ApiProblemDetails.withCode(problem, exception.category(), exception.code());
		problem.setProperty("pricingPreview", PricingPreviewResponse.from(exception.preview()));
		return ResponseEntity.status(status).body(problem);
	}

	@ExceptionHandler(Exception.class)
	private ResponseEntity<ProblemDetail> handleException(Exception exception, HttpServletRequest request) {
		String requestId = UUID.randomUUID().toString();
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setTitle("Ocorreu um erro interno.");
		ApiProblemDetails.withCode(problem, ErrorCategory.INTERNAL_ERROR, "INTERNAL_ERROR");
		problem.setProperty("requestId", requestId);
		log.atError()
			.addKeyValue("event", "api.request.failed")
			.addKeyValue("requestId", requestId)
			.addKeyValue("category", ErrorCategory.INTERNAL_ERROR.name())
			.addKeyValue("code", "INTERNAL_ERROR")
			.addKeyValue("method", request.getMethod())
			.addKeyValue("path", request.getRequestURI())
			.addKeyValue("exceptionType", exception.getClass().getName())
			.setCause(exception)
			.log("Unexpected API request failure");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).header("X-Request-ID", requestId).body(problem);
	}

}
