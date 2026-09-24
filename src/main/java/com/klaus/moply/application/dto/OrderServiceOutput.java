package com.klaus.moply.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.domain.entity.OrderService;

import lombok.Builder;

@Builder
public record OrderServiceOutput(
                UUID id,
                String customer,
                BigDecimal contractedHours,
                BigDecimal hourlyPrice,
                int employeeCount,
                LocalDate serviceDate,
                BigDecimal totalValue,
                BigDecimal individualHour,
                BigDecimal individualPaymentValue) {

        public static OrderServiceOutput fromDomain(OrderService orderService) {
                return OrderServiceOutput.builder()
                                .id(orderService.getId())
                                .customer(orderService.getCustomer().name())
                                .contractedHours(orderService.getContractedHours().value())
                                .hourlyPrice(orderService.getHourlyRate().value())
                                .employeeCount(orderService.getEmployeeCount())
                                .serviceDate(orderService.getServiceDate())
                                .totalValue(orderService.CalculateTotalValue().value())
                                .individualHour(orderService.calculateIndividualHoursPerEmployee().value())
                                .individualPaymentValue(orderService.calculateIndividualPaymentPerEmployee().value())
                                .build();
        }
}