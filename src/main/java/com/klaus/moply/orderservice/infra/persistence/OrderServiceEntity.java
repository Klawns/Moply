package com.klaus.moply.orderservice.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.orderservice.domain.entity.OrderService;

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
@Table(name = "tb_order_service", indexes = { @Index(name = "idx_service_date", columnList = "service_date") })
@Getter
@Setter
@NoArgsConstructor
public class OrderServiceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(nullable = false, name = "contracted_hours")
	private BigDecimal contractedHours;

	@Column(nullable = false, name = "hourly_rate")
	private BigDecimal hourlyRate;

	@Column(nullable = false, name = "employee_count")
	private Integer employeeCount;

	@Column(nullable = false, name = "service_date")
	private LocalDate serviceDate;

	public OrderServiceEntity(UUID id, UUID customerId, BigDecimal contractedHours, BigDecimal hourlyRate,
			Integer employeeCount, LocalDate serviceDate) {
		this.id = id;
		this.customerId = customerId;
		this.contractedHours = contractedHours;
		this.hourlyRate = hourlyRate;
		this.employeeCount = employeeCount;
		this.serviceDate = serviceDate;
	}

	public static OrderServiceEntity fromDomain(OrderService orderService) {

		OrderServiceEntity entity = new OrderServiceEntity();

		entity.id = orderService.getId();
		entity.customerId = orderService.getCustomer().getId();
		entity.contractedHours = orderService.getContractedHours().value();
		entity.hourlyRate = orderService.getHourlyRate().value();
		entity.employeeCount = orderService.getEmployeeCount();
		entity.serviceDate = orderService.getServiceDate();

		return entity;
	}

	public OrderService toDomain(Customer registeredCustomer) {

		return OrderService.restore(id, registeredCustomer, contractedHours, hourlyRate, employeeCount, serviceDate);
	}

}
