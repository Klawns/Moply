package com.klaus.moply.orderservice.infra.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.orderservice.application.usecase.dto.OrderServiceOutput;

import lombok.Builder;

@Builder
public record OrderServiceResponse(UUID id, String customer, BigDecimal totalHours, int employeeCount,
		LocalDate serviceDate, OrderServiceCalculation calculation) {

	public static OrderServiceResponse from(OrderServiceOutput output) {

		OrderServiceCalculation calculation = new OrderServiceCalculation(output.totalAmount(), output.individualHour(),
				output.individualAmount());

		return new OrderServiceResponse(output.id(), output.customer(), output.contractedHours(),
				output.employeeCount(), output.serviceDate(), calculation);
	}
}
