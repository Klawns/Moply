package com.klaus.moply.workorders.domain.vo;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.entity.WorkAssignment;

public record WorkOrderAssignments(List<WorkAssignment> values) {
	public WorkOrderAssignments {
		if (values == null || values.isEmpty()) {
			throw new DomainException("Participações são obrigatórias.");
		}
		var ids = new HashSet<UUID>();
		for (int position = 0; position < values.size(); position++) {
			var assignment = values.get(position);
			if (assignment == null || assignment.inclusionPosition() != position
					|| !ids.add(assignment.collaboratorId())) {
				throw new DomainException("Participações devem ser únicas e ordenadas a partir de zero.");
			}
		}
		values = List.copyOf(values);
	}

	public int count() {
		return values.size();
	}

	public Money totalAmount() {
		var total = values.stream()
			.map(assignment -> assignment.allocatedAmount().value())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Money(total);
	}
}
