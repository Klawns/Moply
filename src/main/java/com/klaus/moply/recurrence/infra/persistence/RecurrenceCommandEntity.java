package com.klaus.moply.recurrence.infra.persistence;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_recurrence_command")
@Getter
@Setter
@NoArgsConstructor
public class RecurrenceCommandEntity {

	@Id
	private UUID id;

	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "family_id")
	private UUID familyId;

	@Column(name = "command_key")
	private String commandKey;

	@Column(name = "content", columnDefinition = "text")
	private String content;

	@Column(name = "actor_id")
	private UUID actorId;

	@Column(name = "recorded_at")
	private java.time.Instant recordedAt;

	@Column(name = "successor_id")
	private UUID successorId;

}
