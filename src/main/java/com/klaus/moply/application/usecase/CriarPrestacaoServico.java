package com.klaus.moply.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.klaus.moply.application.dto.CriarPrestacaoServicoCommand;
import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.PrestacaoServico;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CriarPrestacaoServico implements Usecase<CriarPrestacaoServicoCommand, PrestacaoServicoOutput> {

    private final PrestacaoServicoRepository repo;

    public PrestacaoServicoOutput execute(CriarPrestacaoServicoCommand command) {
        PrestacaoServico prestacaoServico = new PrestacaoServico(
                command.cliente(),
                command.horasContratadas(),
                command.valorHora(),
                command.quantidadeColaboradores(),
                command.data());

        BigDecimal total = prestacaoServico.calcularValorTotal();
        BigDecimal horasInd = prestacaoServico.calcularHorasIndividuaisPorColaborador();
        BigDecimal valorInd = prestacaoServico.calcularValorIndividualPorColaborador();

        prestacaoServico = repo.salvar(prestacaoServico);

        return PrestacaoServicoOutput.builder()
                .id(prestacaoServico.getId())
                .cliente(prestacaoServico.getCliente())
                .horasContratadas(prestacaoServico.getHorasContratadas())
                .valorTotal(total)
                .horasIndividuais(horasInd)
                .valorIndividual(valorInd)
                .data(prestacaoServico.getDataDoServico())
                .quantidadeColaboradores(prestacaoServico.getNumeroDeColaboradores())
                .build();
    }
}
