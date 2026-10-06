package com.klaus.moply.shared.infra.web.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record DatePeriodRequest(@NotNull LocalDate startsOn, LocalDate endsOn) {
}
