package com.klaus.moply.recurrence.application.usecase.dto;

import java.time.LocalDate;
import com.klaus.moply.recurrence.domain.Frequency;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;

public record CreateSeriesInput(Frequency frequency, LocalDate startsOn, LocalDate endsOn, CreateWorkOrderInput work) {
}
