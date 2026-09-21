package com.klaus.moply.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.application.usecase.BuscarPrestacaoServicoPorId;
import com.klaus.moply.domain.PrestacaoServico;

public class BuscarPrestacaoServicoPorIdTest {

    private PrestacaoServicoRepository repo;

    private BuscarPrestacaoServicoPorId useCase;

    @BeforeEach
    void setUp() {
        repo = mock(PrestacaoServicoRepository.class);

        useCase = new BuscarPrestacaoServicoPorId(repo);
    }

    @Test
    void deveBuscarPrestacaoServicoPorId() {

        UUID id = UUID.randomUUID();

        PrestacaoServico prestacao = new PrestacaoServico(
                "João",
                new BigDecimal("4.00"),
                new BigDecimal("11.50"),
                2,
                LocalDate.of(2026, 9, 21));

        when(repo.buscarPorId(id))
                .thenReturn(Optional.of(prestacao));

        PrestacaoServico result = useCase.execute(id);

        assertNotNull(result);
        assertEquals(prestacao, result);

        verify(repo).buscarPorId(id);
    }
}
