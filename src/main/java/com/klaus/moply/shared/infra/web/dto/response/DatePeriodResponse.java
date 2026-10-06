package com.klaus.moply.shared.infra.web.dto.response;

import java.time.LocalDate;

public record DatePeriodResponse(LocalDate startsOn, LocalDate endsOn) {
}
