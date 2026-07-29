package com.cofradias.model;

import jakarta.persistence.CascadeType;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoOptimizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escenario_id", nullable = false)
    private Escenario escenario;

    @Builder.Default
    private LocalDateTime fechaCalculo = LocalDateTime.now();

    private Integer retrasoTotalMinutos;

    private Integer retrasoMaximoMinutos;

    @Builder.Default
    @OneToMany(mappedBy = "resultadoOptimizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResultadoCofradia> resultadosCofradias = new ArrayList<>();
}
