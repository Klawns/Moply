package com.klaus.moply.workflows.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.workflows.application.usecase.dto.RecordRecurrenceCommandInput;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordRecurrenceCommandTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID familyId = UUID.randomUUID();

	private final UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private final Instant now = Instant.parse("2026-10-05T12:00:00Z");

	private final RecurrenceChanges changes = mock(RecurrenceChanges.class);

	private final RecordRecurrenceCommand usecase = new RecordRecurrenceCommand(changes,
			Clock.fixed(now, ZoneOffset.UTC));

	@Test
	void shouldRecordCommandWithFamilyActorTimeAndSuccessor() {
		var successorId = UUID.randomUUID();
		var commandId = usecase.execute(new Context(organizationId),
				new RecordRecurrenceCommandInput(familyId, "key", "content", actorId, successorId));
		verify(changes).save(new RecurrenceChanges.Command(commandId, organizationId, familyId, "key", "content",
				actorId, now, successorId));
	}

}
