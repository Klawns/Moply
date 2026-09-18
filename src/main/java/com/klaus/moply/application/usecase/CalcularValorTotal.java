package com.klaus.moply.application.usecase;

import java.math.BigDecimal;

import com.klaus.moply.domain.PrestacaoServico;

public class CalcularValorTotal {
    public BigDecimal execute(PrestacaoServico prestacaoServico) {
        return prestacaoServico.calcularValorTotal();
    }
}
