package com.klaus.moply.infra.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.klaus.moply.application.ports.PrestacaoServicoRepository;
import com.klaus.moply.domain.PrestacaoServico;
import com.klaus.moply.factory.PrestacaoServicoFactory;

@DataJpaTest
@ActiveProfiles("test")
@Import(PrestacaoServicoRepositoryJpaAdapter.class)
public class PrestacaoServicoRepositoryIntegrationTest {

        @Autowired
        private PrestacaoServicoRepository repo;

        @Test
        @DisplayName("Deve criar prestação de serviço corretamente e buscá-la")
        void shouldSaveAndFindPrestacaoServico() {

                PrestacaoServico prestacaoServico = PrestacaoServicoFactory.createPrestacaoServico();

                PrestacaoServico salvo = repo.salvar(prestacaoServico);

                Optional<PrestacaoServico> resultado = repo.buscarPorId(salvo.getId());

                assertTrue(resultado.isPresent());

                assertEquals("Cliente Teste", resultado.get().getCliente());
                assertTrue(new BigDecimal("4.00").compareTo(resultado.get().getHorasContratadas()) == 0);
                assertTrue(new BigDecimal("11.50").compareTo(resultado.get().getValorHora()) == 0);
                assertEquals(
                                LocalDate.of(2026, 9, 18),
                                resultado.get().getDataDoServico());
        }

        @Test
        @DisplayName("Deve atualizar o cliente de uma prestação de serviço existente em vez de duplicar")
        void shouldUpdatePrestacaoServicoInsteadOfDuplicating() {
                PrestacaoServico prestacaoServico = PrestacaoServicoFactory.createPrestacaoServico();
                PrestacaoServico salvo = repo.salvar(prestacaoServico);
                UUID idOriginal = salvo.getId();

                salvo.alterarCliente("Cliente Atualizado");

                PrestacaoServico atualizado = repo.salvar(salvo);

                assertEquals(idOriginal, atualizado.getId(), "O ID deveria continuar o mesmo");
                assertEquals("Cliente Atualizado", atualizado.getCliente());

                Optional<PrestacaoServico> doBanco = repo.buscarPorId(idOriginal);
                assertTrue(doBanco.isPresent());
                assertEquals("Cliente Atualizado", doBanco.get().getCliente());
        }

        @Test
        @DisplayName("Deve buscar prestações de serviço filtrando pelo nome do cliente")
        void shouldFindPrestacoesServicoByCliente() {
                // Cria dois serviços para o mesmo cliente fictício para garantir a listagem
                PrestacaoServico servico1 = PrestacaoServicoFactory.createPrestacaoServico(); // "Cliente Teste"
                PrestacaoServico servico2 = PrestacaoServicoFactory.createPrestacaoServico(); // "Cliente Teste"

                repo.salvar(servico1);
                repo.salvar(servico2);

                List<PrestacaoServico> resultados = repo.buscarPorCliente("Cliente Teste");

                // Valida se encontrou os registros salvos criados no cenário
                assertTrue(resultados.size() >= 2);
                assertTrue(resultados.stream().allMatch(s -> s.getCliente().equals("Cliente Teste")));
        }

        @Test
        @DisplayName("Deve buscar todas as prestações de serviço agendadas para uma data específica")
        void shouldFindAllPrestacoesServicoByDataDoDia() {
                PrestacaoServico servico = PrestacaoServicoFactory.createPrestacaoServico();
                repo.salvar(servico);

                LocalDate dataDoServico = LocalDate.of(2026, 9, 18); // Data vinda da sua factory
                List<PrestacaoServico> resultados = repo.buscarTodos(dataDoServico);

                assertTrue(resultados.size() >= 1);
                assertEquals(dataDoServico, resultados.get(0).getDataDoServico());
        }

        @Test
        @DisplayName("Deve deletar corretamente uma prestação de serviço pelo ID")
        void shouldDeletePrestacaoServicoById() {
                PrestacaoServico prestacaoServico = PrestacaoServicoFactory.createPrestacaoServico();
                PrestacaoServico salvo = repo.salvar(prestacaoServico);
                UUID id = salvo.getId();

                assertTrue(repo.buscarPorId(id).isPresent());

                repo.deletarPorId(id);

                Optional<PrestacaoServico> deletado = repo.buscarPorId(id);
                assertTrue(deletado.isEmpty());
        }
}
