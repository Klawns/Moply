package com.klaus.moply.recurrence.application.ports;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;

public interface RecurrenceChanges {

	record Command(UUID id, UUID account, UUID family, String key, String content, UUID actor, Instant at,
			UUID successor) {
	}

	record Item(UUID id, UUID account, UUID command, UUID work, long position, String reason, UUID targetSeries,
			java.time.LocalDate serviceDateBefore, java.time.LocalDate serviceDateAfter) {
	}

	Optional<Command> find(UUID account, UUID family, String key);

	Optional<Command> command(UUID account, UUID id);

	void save(Command command);

	void record(Item item);

	void exclude(UUID account, UUID family, long position, UUID work);

	boolean excluded(UUID account, UUID family, long position);

	PageResult<Item> history(UUID account, UUID work, PageQuery page);

}
