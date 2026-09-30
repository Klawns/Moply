package com.klaus.moply.factory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import com.klaus.moply.workorders.domain.entity.*;

public final class WorkOrderFactory {

	public static WorkOrder create(UUID customer, UUID... participants) {
		return WorkOrder.create(customer, null, LocalDate.of(2026, 9, 28), null, null, new BigDecimal("4.00"),
				new BigDecimal("11.50"), List.of(participants), WorkOrderStatus.SCHEDULED);
	}

}
