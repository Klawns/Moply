package com.klaus.moply.recurrence.domain.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.shared.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.*;

class RecurrenceParticipantsTest {

	@Test
	void shouldRejectMissingEmptyNullOrDuplicateParticipants() {
		var id = UUID.randomUUID();
		for (List<UUID> invalid : Arrays.<List<UUID>>asList(null, List.of(), Arrays.asList(id, null),
				List.of(id, id))) {
			var error = assertThrows(DomainException.class, () -> new RecurrenceParticipants(invalid));
			assertEquals("Condições da série inválidas.", error.getMessage());
		}
	}

	@Test
	void shouldPreserveOrderAndDefensivelyCopyParticipants() {
		var first = UUID.randomUUID();
		var second = UUID.randomUUID();
		var input = new ArrayList<>(List.of(second, first));
		var participants = new RecurrenceParticipants(input);
		input.clear();
		assertEquals(List.of(second, first), participants.ids());
		assertThrows(UnsupportedOperationException.class, () -> participants.ids().add(UUID.randomUUID()));
	}

}
