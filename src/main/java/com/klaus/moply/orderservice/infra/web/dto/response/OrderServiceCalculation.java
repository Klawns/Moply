package com.klaus.moply.orderservice.infra.web.dto.response;

import java.math.BigDecimal;

public record OrderServiceCalculation(BigDecimal totalAmount, BigDecimal individualHours, BigDecimal individualAmount) {
}
