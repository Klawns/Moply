package com.klaus.moply.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.entity.OrderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class PrestacaoServicoRepositoryJpaAdapter implements PrestacaoServicoRepository {

    private final PrestacaoServicoJpaRepository repo;

    @Override
    public OrderService salvar(OrderService prestacaoServico) {
        PrestacaoServicoEntity entity = new PrestacaoServicoEntity();

        entity.setId(prestacaoServico.getId());

        entity.setCliente(prestacaoServico.getCustomer());
        entity.setHorasContratadas(prestacaoServico.getHorasContratadas());
        entity.setValorHora(prestacaoServico.getValorHora());
        entity.setNumeroDeColaboradores(prestacaoServico.getNumeroDeColaboradores());
        entity.setDataDoServico(prestacaoServico.getDataDoServico());

        PrestacaoServicoEntity saved = repo.save(entity);

        return toDomain(saved);
    }

    @Override
    public Optional<OrderService> buscarPorId(UUID id) {
        return repo.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<OrderService> buscarPorCliente(String cliente) {
        return repo.findAllByCliente(cliente)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<OrderService> buscarTodos(LocalDate dataDoDia) {
        return repo.findAllByDataDoServico(dataDoDia)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deletarPorId(UUID id) {
        repo.deleteById(id);
    }

    private OrderService toDomain(PrestacaoServicoEntity entity) {

        return OrderService.reconstruir(
                entity.getId(),
                entity.getCliente(),
                entity.getHorasContratadas(),
                entity.getValorHora(),
                entity.getNumeroDeColaboradores(),
                entity.getDataDoServico());
    }
}
