package com.klaus.moply.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;

@Builder
public record CriarPrestacaoServicoInput(
        String cliente,
        BigDecimal horasContratadas,
        BigDecimal valorHora,
        Integer quantidadeColaboradores,
        LocalDate data) {
}