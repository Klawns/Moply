package com.klaus.moply.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuscarPrestacaoServicoPelaData implements Usecase<LocalDate, List<PrestacaoServicoOutput>> {
    private final PrestacaoServicoRepository repo;

    public List<PrestacaoServicoOutput> execute(LocalDate data) {
        return repo.buscarTodos(data).stream()
                .map(PrestacaoServicoOutput::fromDomain)
                .toList();
    }
}
