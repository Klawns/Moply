package com.klaus.moply.infra.web.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarPrestacaoServicoRequest(
                @NotBlank(message = "O cliente não pode estar vazio.") String cliente,
                @Min(value = 1, message = "As horas contratadas devem ser maior que 0.") BigDecimal horasContratadas,
                @Min(value = 1, message = "O valor da hora deve ser maior que 0.") BigDecimal valorHora,
                @Min(value = 1, message = "O número de colaboradores deve ser maior que 0.") Integer quantidadeColaboradores,
                @NotNull(message = "Data não pode estar nulo ou vazia.") LocalDate data) {

}