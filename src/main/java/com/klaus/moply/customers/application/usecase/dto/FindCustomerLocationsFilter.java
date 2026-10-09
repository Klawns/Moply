package com.klaus.moply.customers.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageQuery;

public record FindCustomerLocationsFilter(UUID customerId, PageQuery page) {
}
