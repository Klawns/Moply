package com.klaus.moply.application.usecase;

import java.util.List;

import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuscarPrestacaoServicoPeloCliente implements Usecase<String, List<PrestacaoServicoOutput>> {

    private final PrestacaoServicoRepository repo;

    public List<PrestacaoServicoOutput> execute(String cliente) {
        return repo.buscarPorCliente(cliente).stream()
                .map(PrestacaoServicoOutput::fromDomain)
                .toList();
    }

}
