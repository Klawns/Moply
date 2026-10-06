package com.klaus.moply.payments.infra.web.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PaymentReversalResponse(Instant at, UUID by, String reason) {
}
