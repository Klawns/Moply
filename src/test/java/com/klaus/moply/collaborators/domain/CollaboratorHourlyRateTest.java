package com.klaus.moply.collaborators.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.klaus.moply.collaborators.domain.entities.Collaborator;
import com.klaus.moply.shared.domain.exception.DomainException;

class CollaboratorHourlyRateTest {

	@Test
	void shouldValidateOptionalRatesOnCreationAndRestoration() {
		var org = UUID.randomUUID();
		assertNull(Collaborator.create(org, "Worker", null).getHourlyRate());
		for (String value : List.of("0", "-1", "1.001")) {
			assertThrows(DomainException.class, () -> Collaborator.create(org, "Worker", null, new BigDecimal(value)));
			assertThrows(DomainException.class,
					() -> Collaborator.restore(UUID.randomUUID(), org, "Worker", null, true, 0, new BigDecimal(value)));
		}
		var saved = Collaborator.restore(UUID.randomUUID(), org, "Worker", null, true, 0, new BigDecimal("20"));
		assertEquals(saved.getHourlyRate(), saved.deactivate().getHourlyRate());
		assertEquals(saved.getHourlyRate(), saved.update("Renamed", null).getHourlyRate());
		assertNull(saved.update("Renamed", null, null).getHourlyRate());
	}

}
