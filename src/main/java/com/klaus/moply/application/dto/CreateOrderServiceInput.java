package com.klaus.moply.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;

@Builder
public record CreateOrderServiceInput(
        String customer,
        BigDecimal contractedHours,
        BigDecimal HourlyPrice,
        Integer employeeCount,
        LocalDate serviceDate) {
}