package com.klaus.moply.infra.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.application.dto.PrestacaoServicoOutput;

import lombok.Builder;

@Builder
public record PrestacaoServicoResponse(
                UUID id,
                String cliente,
                BigDecimal horasTotais,
                PrestacaoServicoCalculos calculos,
                LocalDate data,
                int quantidadeColaboradores) {

        public static PrestacaoServicoResponse from(
                        PrestacaoServicoOutput output) {

                PrestacaoServicoCalculos calculos = new PrestacaoServicoCalculos(
                                output.valorTotal(),
                                output.horasIndividuais(),
                                output.valorIndividual());

                return new PrestacaoServicoResponse(
                                output.id(),
                                output.cliente(),
                                output.horasContratadas(),
                                calculos,
                                output.data(),
                                output.quantidadeColaboradores());
        }
}