package com.klaus.moply.domain.factory;

import java.math.BigDecimal;
import java.util.UUID;

import com.klaus.moply.domain.PrestacaoServico;

public class PrestacaoServicoFactory {
    public static PrestacaoServico createPrestacaoServico() {
        return new PrestacaoServico(
                UUID.randomUUID(),
                "Cliente Teste",
                new BigDecimal("4.00"),
                new BigDecimal("11.50"),
                2);
    }
}
