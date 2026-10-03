package com.klaus.moply.recurrence.infra.persistence;

import java.util.UUID;
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

	RecurrenceMemberEntity(RecurrenceSeriesEntity series, UUID collaboratorId, int position) {
		this.id = UUID.randomUUID();
		this.organizationId = series.getOrganizationId();
		this.series = series;
		this.collaboratorId = collaboratorId;
		this.inclusionPosition = position;
	}

}
