package com.klaus.moply.application.usecase;

import java.util.UUID;

import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.exception.PrestacaoServicoNotFoundException;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.entity.OrderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuscarPrestacaoServicoPorId implements Usecase<UUID, PrestacaoServicoOutput> {
    private final PrestacaoServicoRepository repo;

    public PrestacaoServicoOutput execute(UUID id) {
        OrderService prestacaoServico = repo.buscarPorId(id)
                .orElseThrow(() -> new PrestacaoServicoNotFoundException(id));

        return PrestacaoServicoOutput.fromDomain(prestacaoServico);
    }
}
