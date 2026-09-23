package com.klaus.moply.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.klaus.moply.domain.PrestacaoServico;

@Service 
public class CalcularHorasIndividuais {
    public BigDecimal execute(PrestacaoServico prestacaoServico) {
        return prestacaoServico.calcularHorasIndividuaisPorColaborador();
    }
}
