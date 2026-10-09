package com.klaus.moply.customers.application.usecase.dto;

import com.klaus.moply.shared.application.pagination.PageQuery;

public record FindAllLocationsFilter(String query, PageQuery page) {
}
