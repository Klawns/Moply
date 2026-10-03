package com.klaus.moply.recurrence.infra.web;

import com.klaus.moply.recurrence.application.usecase.FindSeries;

import com.klaus.moply.recurrence.application.usecase.CreateSeries;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recurrence-series")
@RequiredArgsConstructor
public class RecurrenceSeriesController {

	private final CreateSeries create;

	private final FindSeries find;

	@PostMapping
	public ResponseEntity<SeriesResponse> create(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestBody CreateSeriesRequest request) {
		var series = create.execute(new Context(principal.getOrganizationId()), request.toInput());
		return ResponseEntity.created(URI.create("/api/v1/recurrence-series/" + series.getId()))
			.body(SeriesResponse.from(series));
	}

	@GetMapping("/{id}")
	public SeriesResponse find(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID id) {
		return SeriesResponse.from(find.execute(new Context(principal.getOrganizationId()), id));
	}

}
