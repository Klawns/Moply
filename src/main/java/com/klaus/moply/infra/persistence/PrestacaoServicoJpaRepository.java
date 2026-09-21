package com.klaus.moply.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface PrestacaoServicoJpaRepository extends CrudRepository<PrestacaoServicoEntity, UUID> {

    List<PrestacaoServicoEntity> findAllByCliente(String cliente);

    List<PrestacaoServicoEntity> findAllByDataDoServico(LocalDate data);
}
