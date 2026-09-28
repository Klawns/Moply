package com.klaus.moply.orderservice.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;

@Builder
public record CreateOrderServiceInput(UUID customerId, BigDecimal contractedHours, BigDecimal HourlyPrice,
		Integer employeeCount, LocalDate serviceDate) {
}
