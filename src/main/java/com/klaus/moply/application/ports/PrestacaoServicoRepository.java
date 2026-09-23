package com.klaus.moply.application.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.domain.entity.OrderService;

public interface PrestacaoServicoRepository {
    OrderService salvar(OrderService prestacaoServico);

    Optional<OrderService> buscarPorId(UUID id);

    List<OrderService> buscarPorCliente(String cliente);

    List<OrderService> buscarTodos(LocalDate dataDoDia);

    void deletarPorId(UUID id);

}
