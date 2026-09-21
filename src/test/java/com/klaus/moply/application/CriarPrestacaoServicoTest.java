package com.klaus.moply.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.klaus.moply.application.dto.CriarPrestacaoServicoCommand;
import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.application.usecase.CalcularHorasIndividuais;
import com.klaus.moply.application.usecase.CalcularValorIndividual;
import com.klaus.moply.application.usecase.CalcularValorTotal;
import com.klaus.moply.application.usecase.CriarPrestacaoServico;
import com.klaus.moply.domain.PrestacaoServico;

public class CriarPrestacaoServicoTest {
        private PrestacaoServicoRepository repo;
        private CalcularHorasIndividuais calcularHorasIndividuais;
        private CalcularValorIndividual calcularValorIndividual;
        private CalcularValorTotal calcularValorTotal;

        private CriarPrestacaoServico useCase;

        @BeforeEach
        void setUp() {
                repo = mock(PrestacaoServicoRepository.class);
                calcularHorasIndividuais = mock(CalcularHorasIndividuais.class);
                calcularValorIndividual = mock(CalcularValorIndividual.class);
                calcularValorTotal = mock(CalcularValorTotal.class);

                useCase = new CriarPrestacaoServico(repo, calcularValorTotal, calcularHorasIndividuais,
                                calcularValorIndividual);
        }

        @Test
        @DisplayName("Deve criar uma prestação corretamente!")
        void shouldCreatePrestacaoServico() {

                var command = new CriarPrestacaoServicoCommand(
                                "João",
                                new BigDecimal("4.00"),
                                new BigDecimal("11.50"),
                                2,
                                LocalDate.of(2026, 9, 21));

                when(calcularValorTotal.execute(any(PrestacaoServico.class)))
                                .thenReturn(new BigDecimal("46.00"));

                when(calcularHorasIndividuais.execute(any(PrestacaoServico.class)))
                                .thenReturn(new BigDecimal("2.00"));

                when(calcularValorIndividual.execute(any(PrestacaoServico.class)))
                                .thenReturn(new BigDecimal("23.00"));

                PrestacaoServicoOutput output = useCase.execute(command);

                assertNotNull(output);

                assertEquals("João", output.cliente());
                assertEquals(new BigDecimal("4.00"), output.horasTotais());
                assertEquals(new BigDecimal("46.00"), output.valorTotal());
                assertEquals(new BigDecimal("2.00"), output.horasIndividuais());
                assertEquals(new BigDecimal("23.00"), output.valorIndividual());
                assertEquals(LocalDate.of(2026, 9, 21), output.data());
                assertEquals(2, output.quantidadeColaboradores());

                verify(repo).salvar(any(PrestacaoServico.class));

                verify(calcularValorTotal)
                                .execute(any(PrestacaoServico.class));

                verify(calcularHorasIndividuais)
                                .execute(any(PrestacaoServico.class));

                verify(calcularValorIndividual)
                                .execute(any(PrestacaoServico.class));
        }
}
