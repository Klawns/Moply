package com.klaus.moply.application.usecase;

import java.util.List;

import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.PrestacaoServico;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuscarPrestacaoServicoPeloCliente {

    private final PrestacaoServicoRepository repo;

    public List<PrestacaoServico> execute(String cliente) {
        return repo.buscarPorCliente(cliente);
    }

}
