package com.klaus.moply.workflows.application.usecase.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import com.klaus.moply.recurrence.domain.vo.SeriesVersion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SelectionContextTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID familyId = UUID.randomUUID();

	private final UUID workId = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private final UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private final OccurrenceSelection selection = new OccurrenceSelection(workId, actorId,
			ChangeScope.THIS_AND_FOLLOWING, "key");

	@Test
	void shouldCopyFamilyVersionsIntoImmutableSelectionContext() {
		var family = family();
		var versions = new ArrayList<>(family.versions());
		var copied = new SelectionContext(selection, organizationId, family.anchor(), 0, versions);
		versions.clear();
		assertEquals(1, copied.versions().size());
		assertThrows(UnsupportedOperationException.class, () -> copied.versions().clear());
	}

	private SelectionContext family() {
		var anchor = mock(RecurrenceSeries.class);
		when(anchor.getLineage()).thenReturn(new SeriesVersion(familyId, null, 0, null));
		return new SelectionContext(selection, organizationId, anchor, 0, List.of(anchor));
	}

}
