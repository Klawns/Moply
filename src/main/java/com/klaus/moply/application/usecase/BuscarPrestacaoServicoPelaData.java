package com.klaus.moply.application.usecase;

import java.time.LocalDate;
import java.util.List;

import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.PrestacaoServico;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuscarPrestacaoServicoPelaData {
    private final PrestacaoServicoRepository repo;

    public List<PrestacaoServico> execute(LocalDate data) {
        return repo.buscarTodos(data);
    }
}
