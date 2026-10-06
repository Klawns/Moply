package com.klaus.moply.recurrence.infra.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.recurrence.application.usecase.CreateSeries;
import com.klaus.moply.recurrence.application.usecase.FindSeries;
import com.klaus.moply.recurrence.infra.web.api.RecurrenceSeriesApi;
import com.klaus.moply.recurrence.infra.web.dto.request.CreateSeriesRequest;
import com.klaus.moply.recurrence.infra.web.dto.response.SeriesResponse;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recurrence-series")
@RequiredArgsConstructor
public class RecurrenceSeriesController implements RecurrenceSeriesApi {

	private final CreateSeries create;

	private final FindSeries find;

	@PostMapping
	@Override
	public ResponseEntity<SeriesResponse> create(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody CreateSeriesRequest request) {
		var series = create.execute(new Context(principal.getOrganizationId()), request.toInput());
		return ResponseEntity.created(URI.create("/api/v1/recurrence-series/" + series.getId()))
			.body(SeriesResponse.from(series));
	}

	@GetMapping("/{id}")
	@Override
	public SeriesResponse find(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		return SeriesResponse.from(find.execute(new Context(principal.getOrganizationId()), id));
	}

}
