package com.klaus.moply.application.usecase;

import java.util.UUID;

import com.klaus.moply.application.exception.PrestacaoServicoNotFoundException;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.entity.OrderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeletarPrestacaoServico {
    private final PrestacaoServicoRepository repo;

    public void execute(UUID id) {
        OrderService prestacaoServico = repo.buscarPorId(id)
                .orElseThrow(() -> new PrestacaoServicoNotFoundException(id));

        repo.deletarPorId(prestacaoServico.getId());
    }
}
