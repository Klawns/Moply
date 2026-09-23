package com.klaus.moply.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.domain.exception.DomainException;
import com.klaus.moply.domain.vo.Customer;
import com.klaus.moply.domain.vo.Money;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class OrderService {
    private UUID id;
    private Customer customer;
    private BigDecimal contractedHours;
    private Money hourlyRate;
    private Integer employeeCount;
    private LocalDate serviceDate;

    public OrderService(String customerName, BigDecimal contractedHours, BigDecimal hourlyRate,
            Integer employeeCount, LocalDate serviceDate) {
        this.customer = new Customer(customerName);
        this.contractedHours = contractedHours;
        this.hourlyRate = new Money(hourlyRate);
        this.employeeCount = employeeCount;
        this.serviceDate = serviceDate;
    }

    public static OrderService reconstruir(
            UUID id,
            String customer,
            BigDecimal contractedHours,
            BigDecimal hourlyRate,
            Integer employeeCount,
            LocalDate serviceDate) {

        OrderService prestacao = new OrderService(
                customer,
                contractedHours,
                hourlyRate,
                employeeCount,
                serviceDate);

        prestacao.id = id;

        return prestacao;
    }

    public Money CalculateTotalValue() {
        return hourlyRate.multiply(contractedHours);
    }

    public BigDecimal calculateIndividualHoursPerEmployee() {
        return contractedHours.divide(BigDecimal.valueOf(employeeCount));
    }

    public Money calculateIndividualPaymentPerEmployee() {
        BigDecimal individualHours = calculateIndividualHoursPerEmployee();
        return hourlyRate.multiply(individualHours);
    }

    public void changeCustomer(String customerName) {
        this.customer = new Customer(customerName);
    }

    public void changeContractedHours(BigDecimal contractedHours) {
        if (contractedHours == null || contractedHours.compareTo(BigDecimal.ZERO) == 0) {
            throw new DomainException("Contracted Hours cant be empty or zero.");
        }

        this.contractedHours = contractedHours;
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
