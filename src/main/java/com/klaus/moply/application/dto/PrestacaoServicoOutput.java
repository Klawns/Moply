package com.klaus.moply.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.domain.entity.OrderService;

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

        public static PrestacaoServicoOutput fromDomain(OrderService prestacaoServico) {
                return PrestacaoServicoOutput.builder()
                                .id(prestacaoServico.getId())
                                .cliente(prestacaoServico.getCustomer())
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