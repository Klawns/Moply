package com.klaus.moply.recurrence.infra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.domain.*;
import com.klaus.moply.recurrence.domain.vo.RecurrenceParticipants;
import com.klaus.moply.recurrence.domain.vo.RecurrencePeriod;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

import static org.junit.jupiter.api.Assertions.*;

class RecurrenceSeriesMappingTest {

	@Test
	void shouldValidatePeriodWhenRestoringPersistedSeries() {
		var template = new WorkTemplate(UUID.randomUUID(), null, null, null, new DurationHours(BigDecimal.ONE),
				new HourlyRate(BigDecimal.TEN), "GBP", new RecurrenceParticipants(List.of(UUID.randomUUID())),
				WorkOrderStatus.SCHEDULED);
		var s = RecurrenceSeries.create(UUID.randomUUID(), Frequency.WEEKLY,
				new RecurrencePeriod(LocalDate.of(2026, 10, 3), null), template);
		var entity = com.klaus.moply.recurrence.infra.persistence.RecurrenceSeriesEntity.from(s);
		assertEquals(s.getPeriod(), entity.toDomain().getPeriod());
		org.springframework.test.util.ReflectionTestUtils.setField(entity, "endsOn", LocalDate.of(2026, 10, 2));
		assertThrows(DomainException.class, entity::toDomain);
		org.springframework.test.util.ReflectionTestUtils.setField(entity, "startsOn", null);
		assertThrows(DomainException.class, entity::toDomain);
		assertThrows(DomainException.class, () -> RecurrenceSeries.restore(s.getId(), s.getOrganizationId(),
				s.getFrequency(), null, s.getTemplate()));
	}

	@Test
	void shouldRestoreParticipantOrderAndRejectInvalidPersistedParticipants() {
		var participants = new RecurrenceParticipants(List.of(UUID.randomUUID(), UUID.randomUUID()));
		var template = new WorkTemplate(UUID.randomUUID(), null, null, null, new DurationHours(BigDecimal.ONE),
				new HourlyRate(BigDecimal.TEN), "GBP", participants, WorkOrderStatus.SCHEDULED);
		var series = RecurrenceSeries.create(UUID.randomUUID(), Frequency.WEEKLY,
				new RecurrencePeriod(LocalDate.of(2026, 10, 3), null), template);
		var entity = com.klaus.moply.recurrence.infra.persistence.RecurrenceSeriesEntity.from(series);
		assertEquals(template, entity.toDomain().getTemplate());
		assertEquals(participants.ids(),
				entity.getMembers().stream().map(member -> member.getCollaboratorId()).toList());
		assertEquals(List.of(0, 1), entity.getMembers().stream().map(member -> member.getInclusionPosition()).toList());
		var member = entity.getMembers().get(1);
		org.springframework.test.util.ReflectionTestUtils.setField(member, "collaboratorId",
				participants.ids().getFirst());
		assertEquals("Condições da série inválidas.",
				assertThrows(DomainException.class, entity::toDomain).getMessage());
		org.springframework.test.util.ReflectionTestUtils.setField(member, "collaboratorId", null);
		assertEquals("Condições da série inválidas.",
				assertThrows(DomainException.class, entity::toDomain).getMessage());
		entity.getMembers().clear();
		assertEquals("Condições da série inválidas.",
				assertThrows(DomainException.class, entity::toDomain).getMessage());
	}

}
