package com.klaus.moply.recurrence.infra.persistence;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_recurrence_exclusion")
@Getter
@Setter
@NoArgsConstructor
public class RecurrenceExclusionEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "family_id")
	private UUID familyId;

	@Column(name = "position")
	private long position;

	@Column(name = "work_id")
	private UUID workId;

}
