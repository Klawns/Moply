package com.klaus.moply.recurrence.infra.persistence;

import com.klaus.moply.shared.domain.vo.Money;

import com.klaus.moply.workorders.domain.vo.WorkOrderAssignments;

import com.klaus.moply.workorders.domain.vo.WorkOrderPricing;

import com.klaus.moply.recurrence.domain.vo.SeriesVersion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import com.klaus.moply.recurrence.domain.*;
import com.klaus.moply.recurrence.domain.vo.RecurrenceParticipants;
import com.klaus.moply.recurrence.domain.vo.RecurrencePeriod;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_recurrence_series")
@Getter
@NoArgsConstructor
public class RecurrenceSeriesEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Frequency frequency;

	@Column(name = "starts_on", nullable = false)
	private LocalDate startsOn;

	@Column(name = "ends_on")
	private LocalDate endsOn;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Column(name = "customer_location_id")
	private UUID customerLocationId;

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

	@Column(name = "total_amount", columnDefinition = "numeric")
	private BigDecimal totalAmount;

	@Column(name = "allocation_policy_version")
	private Integer allocationPolicyVersion;

	@Enumerated(EnumType.STRING)
	@Column(name = "initial_status", nullable = false, length = 20)
	private WorkOrderStatus initialStatus;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "previous_series_id")
	private UUID previousSeriesId;

	@Column(name = "first_position", nullable = false)
	private long firstPosition;

	@Column(name = "until_position")
	private Long untilPosition;

	public void closeAt(Long position) {
		this.untilPosition = position;
	}

	@Version
	private Long version;

	@OneToMany(mappedBy = "series", cascade = CascadeType.ALL)
	@OrderBy("inclusionPosition ASC")
	private List<RecurrenceMemberEntity> members = new ArrayList<>();

	public static RecurrenceSeriesEntity from(RecurrenceSeries s) {
		var e = new RecurrenceSeriesEntity();
		var t = s.getTemplate();
		e.id = s.getId();
		e.familyId = s.getLineage().familyId();
		e.previousSeriesId = s.getLineage().previousSeriesId();
		e.firstPosition = s.getLineage().firstPosition();
		e.untilPosition = s.getLineage().untilPosition();
		e.organizationId = s.getOrganizationId();
		e.frequency = s.getFrequency();
		e.startsOn = s.getPeriod().startsOn();
		e.endsOn = s.getPeriod().endsOn();
		e.customerId = t.customerId();
		e.customerLocationId = t.customerLocationId();
		e.startTime = t.startTime();
		e.description = t.description();
		e.contractedHours = t.contractedHours().value();
		e.hourlyRate = t.hourlyRate().value();
		e.currencyCode = t.currencyCode();
		e.initialStatus = t.initialStatus();
		if (t.frozenPricing() != null) {
			e.totalAmount = t.frozenPricing().pricing().totalAmount().value();
			e.allocationPolicyVersion = t.frozenPricing().pricing().allocationPolicyVersion();
		}
		for (int i = 0; i < t.participants().ids().size(); i++) {
			var member = new RecurrenceMemberEntity(e, t.participants().ids().get(i), i);
			if (t.frozenPricing() != null)
				member.freeze(t.frozenPricing().assignments().values().get(i));
			e.members.add(member);
		}
		return e;
	}

	public RecurrenceSeries toDomain() {
		return RecurrenceSeries.restore(id, organizationId, frequency, new RecurrencePeriod(startsOn, endsOn),
				new WorkTemplate(customerId, customerLocationId, startTime, description,
						new DurationHours(contractedHours), new HourlyRate(hourlyRate), currencyCode,
						new RecurrenceParticipants(
								members.stream().map(RecurrenceMemberEntity::getCollaboratorId).toList()),
						initialStatus, frozenPricing()),
				new SeriesVersion(familyId, previousSeriesId, firstPosition, untilPosition));
	}

	private FrozenWorkPricing frozenPricing() {
		if (totalAmount == null && allocationPolicyVersion == null)
			return null;
		return new FrozenWorkPricing(
				new WorkOrderPricing(new DurationHours(contractedHours), new HourlyRate(hourlyRate), currencyCode,
						new Money(totalAmount), allocationPolicyVersion),
				new WorkOrderAssignments(members.stream().map(RecurrenceMemberEntity::assignment).toList()));
	}

}
