package com.klaus.moply.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_prestaocao_servico")
@Getter
@Setter 
@NoArgsConstructor
public class PrestacaoServicoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "cliente")
    private String cliente;

    @Column(nullable = false, name = "horas_contratadas")
    private BigDecimal horasContratadas;

    @Column(nullable = false, name = "valor_da_hora")
    private BigDecimal valorHora;

    @Column(nullable = false, name = "numero_de_colaboradores")
    private Integer numeroDeColaboradores;

    @Column(nullable = false, name = "data_do_servico")
    private LocalDate dataDoServico;

    public PrestacaoServicoEntity(UUID id, String cliente, BigDecimal horasContratadas, BigDecimal valorHora,
            Integer numeroDeColaboradores, LocalDate dataDoServico) {
        this.cliente = cliente;
        this.horasContratadas = horasContratadas;
        this.valorHora = valorHora;
        this.numeroDeColaboradores = numeroDeColaboradores;
        this.dataDoServico = dataDoServico;
    }

}
