package com.klaus.moply.recurrence.infra.persistence;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_recurrence_change_item")
@Getter
@Setter
@NoArgsConstructor
public class RecurrenceChangeItemEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "command_id")
	private UUID commandId;

	@Column(name = "work_id")
	private UUID workId;

	@Column(name = "position")
	private long position;

	@Column(name = "reason")
	private String reason;

	@Column(name = "target_series_id")
	private UUID targetSeriesId;

	@Column(name = "service_date_before", nullable = false)
	private java.time.LocalDate serviceDateBefore;

	@Column(name = "service_date_after", nullable = false)
	private java.time.LocalDate serviceDateAfter;

}
