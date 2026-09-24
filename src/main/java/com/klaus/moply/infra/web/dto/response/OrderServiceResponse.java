package com.klaus.moply.infra.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.application.usecase.dto.OrderServiceOutput;

import lombok.Builder;

@Builder
public record OrderServiceResponse(
                UUID id,
                String customer,
                BigDecimal totalHours,
                OrderServiceCalculation calculation,
                LocalDate serviceDate,
                int employeeCount) {

        public static OrderServiceResponse from(
                        OrderServiceOutput output) {

                OrderServiceCalculation calculation = new OrderServiceCalculation(
                                output.totalAmount(),
                                output.individualHour(),
                                output.individualAmount());

                return new OrderServiceResponse(
                                output.id(),
                                output.customer(),
                                output.contractedHours(),
                                calculation,
                                output.serviceDate(),
                                output.employeeCount());
        }
}