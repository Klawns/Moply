package com.klaus.moply.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.klaus.moply.domain.exception.DomainException;
import com.klaus.moply.domain.vo.Customer;
import com.klaus.moply.domain.vo.DurationHours;
import com.klaus.moply.domain.vo.Money;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class OrderService {
    private UUID id;
    private Customer customer;
    private DurationHours contractedHours;
    private Money hourlyRate;
    private Integer employeeCount;
    private LocalDate serviceDate;

    private OrderService(UUID id, String customerName, BigDecimal contractedHours, BigDecimal hourlyRate,
            Integer employeeCount, LocalDate serviceDate) {
        this.id = id;
        this.customer = new Customer(customerName);
        this.contractedHours = new DurationHours(contractedHours);
        this.hourlyRate = new Money(hourlyRate);
        this.employeeCount = employeeCount;
        this.serviceDate = Objects.requireNonNull(serviceDate, "Service date cannot be null.");
    }

    public static OrderService create(
            String customer,
            BigDecimal contractedHours,
            BigDecimal hourlyRate,
            Integer employeeCount,
            LocalDate serviceDate) {

        return new OrderService(null, customer, contractedHours, hourlyRate, employeeCount, serviceDate);
    }

    public static OrderService reconstruir(
            UUID id,
            String customer,
            BigDecimal contractedHours,
            BigDecimal hourlyRate,
            Integer employeeCount,
            LocalDate serviceDate) {

        Objects.requireNonNull(id, "ID is required for reconstruction.");
        return new OrderService(id, customer, contractedHours, hourlyRate, employeeCount, serviceDate);
    }

    public Money CalculateTotalValue() {
        return hourlyRate.multiply(contractedHours.value());
    }

    public DurationHours calculateIndividualHoursPerEmployee() {
        return contractedHours.divide(employeeCount);
    }

    public Money calculateIndividualPaymentPerEmployee() {
        DurationHours individualHours = calculateIndividualHoursPerEmployee();
        return hourlyRate.multiply(individualHours.value());
    }

    public void changeCustomer(String customerName) {
        this.customer = new Customer(customerName);
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
