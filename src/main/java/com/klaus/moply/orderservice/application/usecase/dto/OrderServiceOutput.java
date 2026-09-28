package com.klaus.moply.orderservice.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.orderservice.domain.entity.OrderService;

import lombok.Builder;

@Builder
public record OrderServiceOutput(UUID id, String customer, BigDecimal contractedHours, BigDecimal hourlyPrice,
		int employeeCount, LocalDate serviceDate, BigDecimal totalAmount, BigDecimal individualHour,
		BigDecimal individualAmount) {

	public static OrderServiceOutput fromDomain(OrderService orderService) {
		return OrderServiceOutput.builder()
			.id(orderService.getId())
			.customer(orderService.getCustomer().name())
			.contractedHours(orderService.getContractedHours().value())
			.hourlyPrice(orderService.getHourlyRate().value())
			.employeeCount(orderService.getEmployeeCount())
			.serviceDate(orderService.getServiceDate())
			.totalAmount(orderService.CalculateTotalAmount().value())
			.individualHour(orderService.calculateIndividualHoursPerEmployee().value())
			.individualAmount(orderService.calculateIndividualPaymentPerEmployee().value())
			.build();
	}
}
