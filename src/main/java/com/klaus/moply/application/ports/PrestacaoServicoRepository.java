package com.klaus.moply.application.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.klaus.moply.domain.PrestacaoServico;

public interface PrestacaoServicoRepository {
    PrestacaoServico salvar(PrestacaoServico prestacaoServico);

    Optional<PrestacaoServico> buscarPorId(UUID id);

    List<PrestacaoServico> buscarPorCliente(String cliente);

    List<PrestacaoServico> buscarTodos(LocalDate dataDoDia);

    void deletarPorId(UUID id);

}
