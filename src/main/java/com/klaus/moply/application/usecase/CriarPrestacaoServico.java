package com.klaus.moply.application.usecase;

import com.klaus.moply.application.dto.CriarPrestacaoServicoInput;
import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.PrestacaoServico;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CriarPrestacaoServico implements Usecase<CriarPrestacaoServicoInput, PrestacaoServicoOutput> {

    private final PrestacaoServicoRepository repo;

    public PrestacaoServicoOutput execute(CriarPrestacaoServicoInput input) {
        PrestacaoServico prestacaoServico = new PrestacaoServico(
                input.cliente(),
                input.horasContratadas(),
                input.valorHora(),
                input.quantidadeColaboradores(),
                input.data());

        prestacaoServico = repo.salvar(prestacaoServico);

        return PrestacaoServicoOutput.builder()
                .id(prestacaoServico.getId())
                .cliente(prestacaoServico.getCliente())
                .horasContratadas(prestacaoServico.getHorasContratadas())
                .valorTotal(prestacaoServico.calcularValorTotal())
                .horasIndividuais(prestacaoServico.calcularHorasIndividuaisPorColaborador())
                .valorIndividual(prestacaoServico.calcularValorIndividualPorColaborador())
                .data(prestacaoServico.getDataDoServico())
                .quantidadeColaboradores(prestacaoServico.getNumeroDeColaboradores())
                .build();
    }
}
