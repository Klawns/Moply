package com.klaus.moply.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.klaus.moply.domain.exception.DomainException;

public record Money(BigDecimal value) {
    public Money {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            throw new DomainException("Value cant ben equals null or zero.");
        }
    }

    public Money multiply(BigDecimal multiplier) {
        if (multiplier == null) {
            throw new DomainException("Multiplier cant be null.");
        }

        BigDecimal result = this.value.multiply(multiplier);
        return new Money(result);
    }

    public Money divide(BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new DomainException("Divisor cant be null or zero.");
        }

        BigDecimal result = this.value.divide(divisor, 2, RoundingMode.HALF_EVEN);
        return new Money(result);
    }
}
