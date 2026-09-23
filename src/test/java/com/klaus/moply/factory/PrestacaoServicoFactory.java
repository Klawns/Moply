package com.klaus.moply.factory;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.klaus.moply.domain.entity.OrderService;

public class PrestacaoServicoFactory {
    public static OrderService createPrestacaoServico() {
        return new OrderService(
                "Cliente Teste",
                new BigDecimal(4.00),
                new BigDecimal(11.50),
                2,
                LocalDate.of(2026, 9, 18));
    }
}
