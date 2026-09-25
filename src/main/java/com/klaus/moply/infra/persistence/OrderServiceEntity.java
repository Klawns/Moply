package com.klaus.moply.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.domain.entity.OrderService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tb_order_service", 
    indexes = {
        @Index(name = "idx_service_date", columnList = "service_date")
    })
@Getter
@Setter
@NoArgsConstructor
public class OrderServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "customer")
    private String customer;

    @Column(nullable = false, name = "contracted_hours")
    private BigDecimal contractedHours;

    @Column(nullable = false, name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(nullable = false, name = "employee_count")
    private Integer employeeCount;
    
    @Column(nullable = false, name = "service_date")
    private LocalDate serviceDate;

    public OrderServiceEntity(UUID id, String customer, BigDecimal contractedHours, BigDecimal hourlyRate,
            Integer employeeCount, LocalDate serviceDate) {
        this.customer = customer;
        this.contractedHours = contractedHours;
        this.hourlyRate = hourlyRate;
        this.employeeCount = employeeCount;
        this.serviceDate = serviceDate;
    }

    public static OrderServiceEntity fromDomain(OrderService orderService) {

        OrderServiceEntity entity = new OrderServiceEntity();

        entity.id = orderService.getId();
        entity.customer = orderService.getCustomer().name();
        entity.contractedHours = orderService.getContractedHours().value();
        entity.hourlyRate = orderService.getHourlyRate().value();
        entity.employeeCount = orderService.getEmployeeCount();
        entity.serviceDate = orderService.getServiceDate();

        return entity;
    }

    public OrderService toDomain() {

        return OrderService.restore(
                id,
                customer,
                contractedHours,
                hourlyRate,
                employeeCount,
                serviceDate);
    }
}
