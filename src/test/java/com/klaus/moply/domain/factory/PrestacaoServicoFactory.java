package com.klaus.moply.domain.factory;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.klaus.moply.domain.PrestacaoServico;

public class PrestacaoServicoFactory {
    public static PrestacaoServico createPrestacaoServico() {
        return new PrestacaoServico(
                "Cliente Teste",
                new BigDecimal("4.00"),
                new BigDecimal("11.50"),
                2,
                LocalDate.now());
    }
}
