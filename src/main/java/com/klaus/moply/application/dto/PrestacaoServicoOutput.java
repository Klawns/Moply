package com.klaus.moply.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;

@Builder
public record PrestacaoServicoOutput(UUID id, String cliente, BigDecimal horasTotais, BigDecimal valorTotal,
                BigDecimal horasIndividuais, BigDecimal valorIndividual, LocalDate data, int quantidadeColaboradores) {
}