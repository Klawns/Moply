package com.klaus.moply.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.domain.PrestacaoServico;

import lombok.Builder;

@Builder
public record PrestacaoServicoOutput(
                UUID id,
                String cliente,
                BigDecimal horasContratadas,
                BigDecimal valorHora,
                int quantidadeColaboradores,
                LocalDate data,
                BigDecimal valorTotal,
                BigDecimal horasIndividuais,
                BigDecimal valorIndividual) {

        public static PrestacaoServicoOutput fromDomain(PrestacaoServico prestacaoServico) {
                return PrestacaoServicoOutput.builder()
                                .id(prestacaoServico.getId())
                                .cliente(prestacaoServico.getCliente())
                                .horasContratadas(prestacaoServico.getHorasContratadas())
                                .valorHora(prestacaoServico.getValorHora())
                                .quantidadeColaboradores(prestacaoServico.getNumeroDeColaboradores())
                                .data(prestacaoServico.getDataDoServico())
                                .valorTotal(prestacaoServico.calcularValorTotal())
                                .horasIndividuais(prestacaoServico.calcularHorasIndividuaisPorColaborador())
                                .valorIndividual(prestacaoServico.calcularValorIndividualPorColaborador())
                                .build();
        }
}