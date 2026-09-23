package com.klaus.moply.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.application.usecase.BuscarPrestacaoServicoPelaData;
import com.klaus.moply.application.usecase.BuscarPrestacaoServicoPeloCliente;
import com.klaus.moply.application.usecase.BuscarPrestacaoServicoPorId;
import com.klaus.moply.application.usecase.CriarPrestacaoServico;
import com.klaus.moply.application.usecase.DeletarPrestacaoServico;

@Configuration
public class PrestacaoServiceConfig {
    @Bean
    public BuscarPrestacaoServicoPelaData buscarPrestacaoServicoPelaData(PrestacaoServicoRepository repository) {
        return new BuscarPrestacaoServicoPelaData(repository);
    }

    @Bean
    public BuscarPrestacaoServicoPeloCliente buscarPrestacaoServicoPeloCliente(PrestacaoServicoRepository repository) {
        return new BuscarPrestacaoServicoPeloCliente(repository);
    }

    @Bean
    public BuscarPrestacaoServicoPorId buscarPrestacaoServicoPorId(PrestacaoServicoRepository repository) {
        return new BuscarPrestacaoServicoPorId(repository);
    }

    @Bean
    public CriarPrestacaoServico criarPrestacaoServico(PrestacaoServicoRepository repository) {
        return new CriarPrestacaoServico(repository);
    }

    @Bean
    public DeletarPrestacaoServico deletarPrestacaoServico(PrestacaoServicoRepository repository) {
        return new DeletarPrestacaoServico(repository);
    }
}
