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

import com.klaus.moply.application.ports.OrderServiceRepository;
import com.klaus.moply.application.usecase.DeleteOrderService;
import com.klaus.moply.application.usecase.exception.OrderServiceNotFoundException;
import com.klaus.moply.domain.entity.OrderService;
import com.klaus.moply.factory.OrderServiceFactory;

public class DeleteOrderServiceTest {
    private OrderServiceRepository repo;
    private DeleteOrderService useCase;

    @BeforeEach
    void setUp() {
        repo = mock(OrderServiceRepository.class);

        useCase = new DeleteOrderService(repo);
    }

    @Test
    @DisplayName("Deve deletar corretamente uma prestação de serviço pelo ID.")
    void shouldDeletePrestacaoServicoById() {
        OrderService prestacaoServico = OrderServiceFactory.createOrderService();

        UUID id = prestacaoServico.getId();

        when(repo.findById(id))
                .thenReturn(Optional.of(prestacaoServico));

        useCase.execute(id);

        verify(repo).deleteById(id);

    }

    @Test
    @DisplayName("Deve lançar exception quando não existir essa prestação de serviço para ser deletada.")
    void shouldThrowWhenPrestacaoServicoDoesNotExist() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderServiceNotFoundException.class,
                () -> useCase.execute(id));

        verify(repo, never()).deleteById(id);
    }
}
