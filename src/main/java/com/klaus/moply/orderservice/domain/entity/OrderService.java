package com.klaus.moply.orderservice.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.orderservice.domain.vo.DurationHours;
import com.klaus.moply.orderservice.domain.vo.Money;
import com.klaus.moply.shared.domain.exception.DomainException;

import lombok.Getter;

@Getter
public class OrderService {

	private UUID id;

	private Customer customer;

	private DurationHours contractedHours;

	private Money hourlyRate;

	private Integer employeeCount;

	private LocalDate serviceDate;

	private OrderService(UUID id, Customer customer, BigDecimal contractedHours, BigDecimal hourlyRate,
			Integer employeeCount, LocalDate serviceDate) {
		this.id = id;
		validateCustomer(customer);
		this.customer = customer;
		this.contractedHours = new DurationHours(contractedHours);
		this.hourlyRate = new Money(hourlyRate);
		this.employeeCount = employeeCount;
		this.serviceDate = Objects.requireNonNull(serviceDate, "Service date cannot be null.");
	}

	public static OrderService create(Customer customer, BigDecimal contractedHours, BigDecimal hourlyRate,
			Integer employeeCount, LocalDate serviceDate) {

		return new OrderService(null, customer, contractedHours, hourlyRate, employeeCount, serviceDate);
	}

	public static OrderService restore(UUID id, Customer customer, BigDecimal contractedHours, BigDecimal hourlyRate,
			Integer employeeCount, LocalDate serviceDate) {

		Objects.requireNonNull(id, "ID is required for reconstruction.");
		return new OrderService(id, customer, contractedHours, hourlyRate, employeeCount, serviceDate);
	}

	public Money CalculateTotalAmount() {
		return hourlyRate.multiply(contractedHours.value());
	}

	public DurationHours calculateIndividualHoursPerEmployee() {
		return contractedHours.divide(employeeCount);
	}

	public Money calculateIndividualPaymentPerEmployee() {
		DurationHours individualHours = calculateIndividualHoursPerEmployee();
		return hourlyRate.multiply(individualHours.value());
	}

	public void changeCustomer(Customer customer) {
		validateCustomer(customer);
		this.customer = customer;
	}

	private static void validateCustomer(Customer customer) {
		if (customer == null || customer.getId() == null) {
			throw new DomainException("Um cliente cadastrado é obrigatório.");
		}
	}

	public void changeContractedHours(BigDecimal contractedHours) {
		this.contractedHours = new DurationHours(contractedHours);
	}

	public void changeHourlyRate(BigDecimal hourlyRate) {
		this.hourlyRate = new Money(hourlyRate);
	}

	public void changeEmployeeCount(Integer employeeCount) {
		if (employeeCount == null || employeeCount == 0) {
			throw new DomainException("Employee Count cant be empty or zero.");
		}
		this.employeeCount = employeeCount;
	}

	public void changeServiceDate(LocalDate serviceDate) {
		if (serviceDate == null) {
			throw new DomainException("Service Date cant be empty or zero.");
		}
		this.serviceDate = serviceDate;
	}

}
