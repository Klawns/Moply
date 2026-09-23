package com.klaus.moply.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.klaus.moply.application.dto.CriarPrestacaoServicoInput;
import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.application.usecase.CriarPrestacaoServico;
import com.klaus.moply.domain.entity.OrderService;

public class CriarPrestacaoServicoTest {
        private PrestacaoServicoRepository repo;

        private CriarPrestacaoServico useCase;

        @BeforeEach
        void setUp() {
                repo = mock(PrestacaoServicoRepository.class);

                useCase = new CriarPrestacaoServico(repo);
        }

        @Test
        @DisplayName("Deve criar uma prestação.")
        void shouldCreatePrestacaoServico() {
                var command = new CriarPrestacaoServicoInput(
                                "João",
                                new BigDecimal("4.00"),
                                new BigDecimal("11.50"),
                                2,
                                LocalDate.of(2026, 9, 21));

                UUID idGerado = UUID.randomUUID();

                when(repo.salvar(any(OrderService.class)))
                                .thenAnswer(invocation -> {
                                        OrderService dominioPassado = invocation.getArgument(0);
                                        return OrderService.reconstruir(
                                                        idGerado,
                                                        dominioPassado.getCustomer(),
                                                        dominioPassado.getHorasContratadas(),
                                                        dominioPassado.getValorHora(),
                                                        dominioPassado.getNumeroDeColaboradores(),
                                                        dominioPassado.getDataDoServico());
                                });

                PrestacaoServicoOutput output = useCase.execute(command);

                assertNotNull(output);
                assertEquals(idGerado, output.id());
                assertEquals("João", output.cliente());
                assertEquals(new BigDecimal("4.00"), output.horasContratadas());
                assertEquals(2, output.quantidadeColaboradores());
                assertEquals(LocalDate.of(2026, 9, 21), output.data());

                assertEquals(new BigDecimal("46.00"), output.valorTotal());
                assertEquals(new BigDecimal("2.00"), output.horasIndividuais());
                assertEquals(new BigDecimal("23.00"), output.valorIndividual());

                verify(repo).salvar(any(OrderService.class));
        }

}
