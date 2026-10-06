package com.klaus.moply.workorders.infra.web.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.recurrence.application.usecase.FindOccurrenceHistory;
import com.klaus.moply.recurrence.infra.web.dto.response.OccurrenceHistoryResponse;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.infra.web.PageQueryRequest;
import com.klaus.moply.shared.infra.web.dto.PageResponse;
import com.klaus.moply.workorders.infra.web.api.WorkOrderRecurrenceHistoryApi;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderRecurrenceHistoryController implements WorkOrderRecurrenceHistoryApi {

	private final FindOccurrenceHistory history;

	@GetMapping("/{id}/recurrence-history")
	@Override
	public PageResponse<OccurrenceHistoryResponse> history(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID id, @RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size, @RequestParam(required = false) String sort,
			@RequestParam(required = false) String direction) {
		return PageResponse.from(
				history.execute(new Context(principal.getOrganizationId()),
						new FindOccurrenceHistory.Filter(id, PageQueryRequest.toQuery(page, size, sort, direction))),
				OccurrenceHistoryResponse::from);
	}

}
