package com.klaus.moply.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.klaus.moply.application.exception.PrestacaoServicoNotFoundException;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.PrestacaoServico;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service 
public class DeletarPrestacaoServico {
    private final PrestacaoServicoRepository repo;

    public void execute(UUID id) {
        PrestacaoServico prestacaoServico = repo.buscarPorId(id)
                .orElseThrow(() -> new PrestacaoServicoNotFoundException(id));

        repo.deletarPorId(prestacaoServico.getId());
    }
}
