package com.klaus.moply.workflows.application.usecase;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.workflows.application.usecase.dto.CheckRecurrenceCommandReplayInput;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckRecurrenceCommandReplayTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID familyId = UUID.randomUUID();

	private final UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private final Instant now = Instant.parse("2026-10-05T12:00:00Z");

	private final RecurrenceChanges changes = mock(RecurrenceChanges.class);

	private final CheckRecurrenceCommandReplay usecase = new CheckRecurrenceCommandReplay(changes);

	@Test
	void shouldReplayOnlyIdenticalCommandsInSameOrganizationAndFamily() {
		assertFalse(usecase.execute(new Context(organizationId),
				new CheckRecurrenceCommandReplayInput(familyId, "key", "content")));
		var prior = new RecurrenceChanges.Command(UUID.randomUUID(), organizationId, familyId, "key", "content",
				actorId, now, null);
		when(changes.find(organizationId, familyId, "key")).thenReturn(Optional.of(prior));
		assertTrue(usecase.execute(new Context(organizationId),
				new CheckRecurrenceCommandReplayInput(familyId, "key", "content")));
		assertThrows(WorkOrderStateException.class, () -> usecase.execute(new Context(organizationId),
				new CheckRecurrenceCommandReplayInput(familyId, "key", "different")));
		verify(changes, never()).save(any());
	}

}
