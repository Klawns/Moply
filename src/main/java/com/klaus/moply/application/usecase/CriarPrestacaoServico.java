package com.klaus.moply.application.usecase;

import com.klaus.moply.application.dto.CriarPrestacaoServicoCommand;
import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.PrestacaoServico;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CriarPrestacaoServico {

    private final PrestacaoServicoRepository repo;
    private final CalcularValorTotal calcularValorTotal;
    private final CalcularHorasIndividuais calcularHorasIndividuais;
    private final CalcularValorIndividual calcularValorIndividual;

    public PrestacaoServicoOutput execute(CriarPrestacaoServicoCommand command) {
        PrestacaoServico prestacaoServico = new PrestacaoServico(
                command.cliente(),
                command.horasContratadas(),
                command.valorHora(),
                command.quantidadeColaboradores(),
                command.data());

        repo.salvar(prestacaoServico);

        return PrestacaoServicoOutput.builder()
                .id(prestacaoServico.getId())
                .cliente(prestacaoServico.getCliente())
                .horasTotais(prestacaoServico.getHorasContratadas())
                .valorTotal(calcularValorTotal.execute(prestacaoServico))
                .horasIndividuais(calcularHorasIndividuais.execute(prestacaoServico))
                .valorIndividual(calcularValorIndividual.execute(prestacaoServico))
                .data(prestacaoServico.getDataDoServico())
                .quantidadeColaboradores(prestacaoServico.getNumeroDeColaboradores())
                .build();   
    }
}
