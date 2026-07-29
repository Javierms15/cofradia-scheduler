package com.cofradias.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoCofradia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resultado_optimizacion_id", nullable = false)
    private ResultadoOptimizacion resultadoOptimizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cofradia_id", nullable = false)
    private Cofradia cofradia;

    private Integer ordenAsignado;

    private LocalTime horaEntradaCalculada;

    private LocalTime horaSalidaCalculada;

    private Integer retrasoMinutos;
}
