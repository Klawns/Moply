package com.klaus.moply.infra.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.application.dto.CriarPrestacaoServicoCommand;
import com.klaus.moply.application.dto.PrestacaoServicoOutput;
import com.klaus.moply.application.usecase.BuscarPrestacaoServicoPelaData;
import com.klaus.moply.application.usecase.BuscarPrestacaoServicoPeloCliente;
import com.klaus.moply.application.usecase.BuscarPrestacaoServicoPorId;
import com.klaus.moply.application.usecase.CriarPrestacaoServico;
import com.klaus.moply.application.usecase.DeletarPrestacaoServico;
import com.klaus.moply.infra.web.dto.request.CriarPrestacaoServicoRequest;
import com.klaus.moply.infra.web.dto.response.PrestacaoServicoResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/prestacao-servico")
@RequiredArgsConstructor
public class PrestacaoServiceController {
    private final CriarPrestacaoServico criar;
    private final BuscarPrestacaoServicoPelaData buscarPelaData;
    private final BuscarPrestacaoServicoPeloCliente buscarPeloCliente;
    private final BuscarPrestacaoServicoPorId buscarPorId;
    private final DeletarPrestacaoServico deletar;

    @PostMapping
    public ResponseEntity<PrestacaoServicoResponse> criarPrestacaoServico(
            @RequestBody @Valid CriarPrestacaoServicoRequest request) {

        CriarPrestacaoServicoCommand command = CriarPrestacaoServicoCommand.builder()
                .cliente(request.cliente())
                .horasContratadas(request.horasContratadas())
                .valorHora(request.valorHora())
                .quantidadeColaboradores(request.quantidadeColaboradores())
                .data(request.data())
                .build();

        var prestacao = criar.execute(command);

        var response = PrestacaoServicoResponse.from(prestacao);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PrestacaoServicoResponse>> listar(
            @RequestParam LocalDate data,
            @RequestParam(required = false) String cliente) {

        List<PrestacaoServicoOutput> prestacaoServico;

        if (cliente != null) {
            prestacaoServico = buscarPeloCliente.execute(cliente);
        } else {
            prestacaoServico = buscarPelaData.execute(data);
        }

        List<PrestacaoServicoResponse> resposta = prestacaoServico.stream()
                .map(PrestacaoServicoResponse::from)
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/{prestacao-servico-id}")
    public ResponseEntity<PrestacaoServicoResponse> listarPorId(
            @PathVariable UUID prestacaoId) {

        PrestacaoServicoOutput output = buscarPorId.execute(prestacaoId);

        return ResponseEntity.ok(
                PrestacaoServicoResponse.from(output));
    }

    @DeleteMapping("/{prestacao-servico-id}")
    public ResponseEntity<Void> deletarPorId(@PathVariable UUID id) {
        deletar.execute(id);

        return ResponseEntity.noContent().build();
    }

}
