package com.klaus.moply.workflows.application.usecase.support;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.klaus.moply.workflows.application.usecase.dto.CancelOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.OccurrenceSelection;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleOccurrenceInput;

public final class RecurrenceCommandContent {

	private RecurrenceCommandContent() {
	}

	public static String cancellationContent(CancelOccurrenceInput input) {
		var fields = new ArrayList<Object>();
		var confirmations = input.confirmations()
			.stream()
			.sorted(Comparator.comparing(confirmation -> confirmation.paymentId().toString()))
			.toList();
		for (var confirmation : confirmations) {
			fields.add(confirmation.paymentId());
			fields.add(confirmation.confirmNoMoneyReceived());
			fields.add(confirmation.reason());
		}
		return encodeContent("CANCEL", input.selection(), fields.toArray());
	}

	public static String reschedulingContent(RescheduleOccurrenceInput input) {
		return encodeContent("RESCHEDULE", input.selection(), input.serviceDate(), input.startTime());
	}

	/** Preserve the stored encoding so existing idempotency keys remain replayable. */
	private static String encodeContent(String action, OccurrenceSelection selection, Object... fields) {
		var values = new ArrayList<String>();
		values.add(action);
		values.add(selection.workId().toString());
		values.add(selection.actorId().toString());
		values.add(selection.scope().name());
		for (var field : fields) {
			values.add(field == null ? "null" : field.toString());
		}
		return values.stream()
			.map(value -> Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)))
			.collect(Collectors.joining("."));
	}

}
