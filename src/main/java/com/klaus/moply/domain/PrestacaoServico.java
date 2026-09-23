package com.klaus.moply.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class PrestacaoServico {
    private UUID id;
    private String cliente;
    private BigDecimal horasContratadas;
    private BigDecimal valorHora;
    private Integer numeroDeColaboradores;
    private LocalDate dataDoServico;

    public PrestacaoServico(String cliente, BigDecimal horasContratadas, BigDecimal valorHora,
            Integer numeroDeColaboradores, LocalDate dataDoServico) {
        this.cliente = cliente;
        this.horasContratadas = horasContratadas;
        this.valorHora = valorHora;
        this.numeroDeColaboradores = numeroDeColaboradores;
        this.dataDoServico = dataDoServico;
    }

    public static PrestacaoServico reconstruir(
            UUID id,
            String cliente,
            BigDecimal horasContratadas,
            BigDecimal valorHora,
            Integer numeroDeColaboradores,
            LocalDate dataDoServico) {

        PrestacaoServico prestacao = new PrestacaoServico(
                cliente,
                horasContratadas,
                valorHora,
                numeroDeColaboradores,
                dataDoServico);

        prestacao.id = id;

        return prestacao;
    }

    public BigDecimal calcularValorTotal() {
        return valorHora.multiply(horasContratadas).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularHorasIndividuaisPorColaborador() {
        return horasContratadas.divide(BigDecimal.valueOf(numeroDeColaboradores), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularValorIndividualPorColaborador() {
        return calcularHorasIndividuaisPorColaborador().multiply(valorHora).setScale(2, RoundingMode.HALF_UP);
    }

    public void alterarCliente(String cliente) {
        this.cliente = Objects.requireNonNull(cliente, "Cliente não pode ser nulo.");
    }

    public void alterarHorasContratadas(BigDecimal horasContratadas) {
        this.horasContratadas = Objects.requireNonNull(horasContratadas, "Horas contratadas não podem ser nulas.");
    }

    public void alterarValorHora(BigDecimal valorHora) {
        this.valorHora = Objects.requireNonNull(valorHora, "Valor por hora não pode ser nulo.");
    }

    public void alterarNumeroDeColaboradores(Integer numeroDeColaboradores) {
        this.numeroDeColaboradores = Objects.requireNonNull(numeroDeColaboradores,
                "Número de colaboradores não pode ser nulo.");
    }

    public void alterarDataDoServico(LocalDate dataDoServico) {
        this.dataDoServico = Objects.requireNonNull(dataDoServico, "Data do serviço não pode ser nula.");
    }

}
