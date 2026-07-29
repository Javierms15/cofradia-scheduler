package com.cofradias.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cofradia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escenario_id", nullable = false)
    private Escenario escenario;

    @Column(nullable = false)
    private String nombre;

    private Integer numNazarenos;

    private Double longitudCortejoMetros;

    private Double velocidadMarchaMetrosMinuto;

    private LocalTime horaSalidaEstimada;

    private LocalTime horaEncierroLimite;

    private LocalTime horaOficialPaso;

    @Builder.Default
    @OneToMany(mappedBy = "cofradia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recorrido> recorridos = new ArrayList<>();
}
