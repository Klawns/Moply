package com.klaus.moply.recurrence.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageQuery;

public record FindOccurrenceHistoryFilter(UUID workOrderId, PageQuery page) {
}
