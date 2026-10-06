package com.klaus.moply.recurrence.infra.web.dto.response;

import java.time.LocalDate;

public record ServiceDateChangeResponse(LocalDate before, LocalDate after) {
}
