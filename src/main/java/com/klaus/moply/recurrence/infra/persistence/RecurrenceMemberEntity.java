package com.klaus.moply.recurrence.infra.persistence;

import java.util.UUID;
import java.math.BigDecimal;
import com.klaus.moply.workorders.domain.entity.WorkAssignment;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import com.klaus.moply.shared.domain.vo.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_recurrence_member")
@Getter
@NoArgsConstructor
public class RecurrenceMemberEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "series_id", nullable = false)
	private RecurrenceSeriesEntity series;

	@Column(name = "collaborator_id", nullable = false)
	private UUID collaboratorId;

	@Column(name = "inclusion_position", nullable = false)
	private int inclusionPosition;

	@Column(name = "allocated_amount", columnDefinition = "numeric")
	private BigDecimal allocatedAmount;

	@Column(name = "applied_hourly_rate", columnDefinition = "numeric")
	private BigDecimal appliedHourlyRate;

	@Column(name = "fixed_rate")
	private Boolean fixedRate;

	@Column(name = "base_amount", columnDefinition = "numeric")
	private BigDecimal baseAmount;

	@Column(name = "surplus_amount", columnDefinition = "numeric")
	private BigDecimal surplusAmount;

	public void freeze(WorkAssignment assignment) {
		allocatedAmount = assignment.allocatedAmount().value();
		appliedHourlyRate = assignment.appliedHourlyRate() == null ? null : assignment.appliedHourlyRate().value();
		fixedRate = assignment.fixedRate();
		baseAmount = assignment.baseAmount() == null ? null : assignment.baseAmount().value();
		surplusAmount = assignment.surplusAmount() == null ? null : assignment.surplusAmount().value();
	}

	public WorkAssignment assignment() {
		return new WorkAssignment(collaboratorId, inclusionPosition, new Money(allocatedAmount),
				appliedHourlyRate == null ? null : new HourlyRate(appliedHourlyRate), fixedRate,
				baseAmount == null ? null : new Money(baseAmount),
				surplusAmount == null ? null : new Money(surplusAmount));
	}

	RecurrenceMemberEntity(RecurrenceSeriesEntity series, UUID collaboratorId, int position) {
		this.id = UUID.randomUUID();
		this.organizationId = series.getOrganizationId();
		this.series = series;
		this.collaboratorId = collaboratorId;
		this.inclusionPosition = position;
	}

}
