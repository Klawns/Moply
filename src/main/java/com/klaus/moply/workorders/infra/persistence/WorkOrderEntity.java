package com.klaus.moply.workorders.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_order_service")
@Getter
@Setter
@NoArgsConstructor
public class WorkOrderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@Column(name = "recurrence_series_id")
	private UUID recurrenceSeriesId;

	@Column(name = "occurrence_date")
	private LocalDate occurrenceDate;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(name = "customer_location_id")
	private UUID customerLocationId;

	@Column(name = "service_date", nullable = false)
	private LocalDate serviceDate;

	@Column(name = "start_time")
	private LocalTime startTime;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "contracted_hours", nullable = false, columnDefinition = "numeric")
	private BigDecimal contractedHours;

	@Column(name = "hourly_rate", nullable = false, columnDefinition = "numeric")
	private BigDecimal hourlyRate;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "total_amount", nullable = false, columnDefinition = "numeric")
	private BigDecimal totalAmount;

	@Column(name = "allocation_policy_version", nullable = false)
	private int allocationPolicyVersion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WorkOrderStatus status;

	@Version
	@Column(nullable = false)
	private Long version;

	@OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL)
	@OrderBy("inclusionPosition ASC")
	private List<WorkAssignmentEntity> assignments = new ArrayList<>();

	public static WorkOrderEntity from(UUID organizationId, WorkOrder work) {
		var e = new WorkOrderEntity();
		e.organizationId = organizationId;
		e.customerId = work.customerId();
		e.customerLocationId = work.customerLocationId();
		e.serviceDate = work.serviceDate();
		e.startTime = work.startTime();
		e.description = work.description();
		e.contractedHours = work.contractedHours().value();
		e.hourlyRate = work.hourlyRate().value();
		e.currencyCode = work.currencyCode();
		e.totalAmount = work.totalAmount().value();
		e.allocationPolicyVersion = work.allocationPolicyVersion();
		e.status = work.status();
		if (work.occurrence() != null) {
			e.recurrenceSeriesId = work.occurrence().seriesId();
			e.occurrenceDate = work.occurrence().originalDate();
		}
		for (var a : work.assignments())
			e.assignments.add(WorkAssignmentEntity.from(e, a));
		return e;
	}

	public WorkOrder toDomain() {
		return new WorkOrder(id, customerId, customerLocationId, serviceDate, startTime, description,
				new DurationHours(contractedHours), new HourlyRate(hourlyRate), currencyCode, new Money(totalAmount),
				allocationPolicyVersion, status, version,
				assignments.stream().map(WorkAssignmentEntity::toDomain).toList(),
				recurrenceSeriesId == null ? null : new com.klaus.moply.workorders.domain.vo.OccurrenceIdentity(
						recurrenceSeriesId, occurrenceDate));
	}

}
