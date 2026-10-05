package com.klaus.moply.workorders.infra.persistence;

import com.klaus.moply.workorders.domain.vo.HourlyRate;

import java.math.BigDecimal;
import java.util.UUID;

import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.entity.WorkAssignment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_work_assignment", uniqueConstraints = {
		@UniqueConstraint(name = "uk_assignment_collaborator", columnNames = { "work_order_id", "collaborator_id" }),
		@UniqueConstraint(name = "uk_assignment_position", columnNames = { "work_order_id", "inclusion_position" }) })
@Getter
@Setter
@NoArgsConstructor
public class WorkAssignmentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "organization_id", nullable = false, updatable = false)
	private UUID organizationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "work_order_id", nullable = false)
	private WorkOrderEntity workOrder;

	@Column(name = "collaborator_id", nullable = false)
	private UUID collaboratorId;

	@Column(name = "inclusion_position", nullable = false)
	private int inclusionPosition;

	@Column(name = "allocated_amount", nullable = false, columnDefinition = "numeric")
	private BigDecimal allocatedAmount;

	@Column(name = "applied_hourly_rate", columnDefinition = "numeric")
	private BigDecimal appliedHourlyRate;

	@Column(name = "fixed_rate")
	private Boolean fixedRate;

	@Column(name = "base_amount", columnDefinition = "numeric")
	private BigDecimal baseAmount;

	@Column(name = "surplus_amount", columnDefinition = "numeric")
	private BigDecimal surplusAmount;

	static WorkAssignmentEntity from(WorkOrderEntity work, WorkAssignment a) {
		var e = new WorkAssignmentEntity();
		e.organizationId = work.getOrganizationId();
		e.workOrder = work;
		e.collaboratorId = a.collaboratorId();
		e.inclusionPosition = a.inclusionPosition();
		e.allocatedAmount = a.allocatedAmount().value();
		e.appliedHourlyRate = a.appliedHourlyRate() == null ? null : a.appliedHourlyRate().value();
		e.fixedRate = a.fixedRate();
		e.baseAmount = a.baseAmount() == null ? null : a.baseAmount().value();
		e.surplusAmount = a.surplusAmount() == null ? null : a.surplusAmount().value();
		return e;
	}

	WorkAssignment toDomain() {
		return new WorkAssignment(collaboratorId, inclusionPosition, new Money(allocatedAmount),
				appliedHourlyRate == null ? null : new HourlyRate(appliedHourlyRate), fixedRate,
				baseAmount == null ? null : new Money(baseAmount),
				surplusAmount == null ? null : new Money(surplusAmount));
	}

}
