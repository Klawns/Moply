package com.klaus.moply.application.usecase;

import java.util.UUID;

import com.klaus.moply.application.exception.PrestacaoServicoNotFoundException;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.PrestacaoServico;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuscarPrestacaoServicoPorId {
    private final PrestacaoServicoRepository repo;

    public PrestacaoServico execute(UUID id) {
        return repo.buscarPorId(id)
                .orElseThrow(() -> new PrestacaoServicoNotFoundException(id));
    }
}
