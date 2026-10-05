package com.klaus.moply.factory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import com.klaus.moply.workorders.domain.entity.*;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import com.klaus.moply.workorders.domain.vo.WorkOrderDescription;
import com.klaus.moply.workorders.domain.vo.WorkOrderSchedule;

public final class WorkOrderFactory {

	public static WorkOrder create(UUID customer, UUID... participants) {
		return WorkOrder.create(customer, null, new WorkOrderSchedule(LocalDate.of(2026, 9, 28), null),
				new WorkOrderDescription(null), new DurationHours(new BigDecimal("4.00")),
				new HourlyRate(new BigDecimal("11.50")), List.of(participants), WorkOrderStatus.SCHEDULED);
	}

}
