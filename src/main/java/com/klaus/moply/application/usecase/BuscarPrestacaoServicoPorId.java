package com.klaus.moply.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.exception.PrestacaoServicoNotFoundException;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.PrestacaoServico;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuscarPrestacaoServicoPorId {
    private final PrestacaoServicoRepository repo;

    public PrestacaoServicoOutput execute(UUID id) {
        PrestacaoServico prestacaoServico = repo.buscarPorId(id)
                .orElseThrow(() -> new PrestacaoServicoNotFoundException(id));

        return PrestacaoServicoOutput.fromDomain(prestacaoServico);
    }
}
