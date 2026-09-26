package com.klaus.moply.infra.web.dto.response;

import java.math.BigDecimal;

public record OrderServiceCalculation(BigDecimal totalAmount, BigDecimal individualHours, BigDecimal individualAmount) {
}
