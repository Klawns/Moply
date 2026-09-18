package com.klaus.moply.domain.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.klaus.moply.domain.factory.PrestacaoServicoFactory;

public class CalculosPrestacaoServicoTest {
    @Test
    @DisplayName("Deve calcular o valor total de uma prestação de serviço")
    public void deveCalcularValorTotalPrestacaoServico() {
        var prestacaoServico = PrestacaoServicoFactory.createPrestacaoServico();
        var valorTotal = prestacaoServico.calcularValorTotal();
        assertEquals(new BigDecimal("46.00"), valorTotal);
    }

    @Test
    @DisplayName("Deve calcular as horas individuais por colaborador de uma prestação de serviço")
    public void deveCalcularHorasIndividuaisPorColaborador() {
        var prestacaoServico = PrestacaoServicoFactory.createPrestacaoServico();
        var horasIndividuais = prestacaoServico.calcularHorasIndividuaisPorColaborador();
        assertEquals(new BigDecimal("2.00"), horasIndividuais);
    }

    @Test
    @DisplayName("Deve calcular o valor individual por colaborador de uma prestação de serviço")
    public void deveCalcularValorIndividualPorColaborador() {
        var prestacaoServico = PrestacaoServicoFactory.createPrestacaoServico();
        var valorIndividual = prestacaoServico.calcularValorIndividualPorColaborador();
        assertEquals(new BigDecimal("23.00"), valorIndividual);
    }

}
