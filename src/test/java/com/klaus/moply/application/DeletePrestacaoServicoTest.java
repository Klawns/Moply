package com.klaus.moply.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.klaus.moply.application.exception.PrestacaoServicoNotFoundException;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.application.usecase.DeletarPrestacaoServico;
import com.klaus.moply.domain.entity.OrderService;
import com.klaus.moply.factory.PrestacaoServicoFactory;

public class DeletePrestacaoServicoTest {
    private PrestacaoServicoRepository repo;
    private DeletarPrestacaoServico useCase;

    @BeforeEach
    void setUp() {
        repo = mock(PrestacaoServicoRepository.class);

        useCase = new DeletarPrestacaoServico(repo);
    }

    @Test
    @DisplayName("Deve deletar corretamente uma prestação de serviço pelo ID.")
    void shouldDeletePrestacaoServicoById() {
        OrderService prestacaoServico = PrestacaoServicoFactory.createPrestacaoServico();

        UUID id = prestacaoServico.getId();

        when(repo.buscarPorId(id))
                .thenReturn(Optional.of(prestacaoServico));

        useCase.execute(id);

        verify(repo).deletarPorId(id);

    }

    @Test
    @DisplayName("Deve lançar exception quando não existir essa prestação de serviço para ser deletada.")
    void shouldThrowWhenPrestacaoServicoDoesNotExist() {

        UUID id = UUID.randomUUID();

        when(repo.buscarPorId(id))
                .thenReturn(Optional.empty());

        assertThrows(
                PrestacaoServicoNotFoundException.class,
                () -> useCase.execute(id));

        verify(repo, never()).deletarPorId(id);
    }
}
