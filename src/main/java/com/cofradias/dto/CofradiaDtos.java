package com.cofradias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;

public class CofradiaDtos {

    public record CreateRequest(
            @NotBlank String nombre,
            @Positive Integer numNazarenos,
            @Positive Double longitudCortejoMetros,
            @Positive Double velocidadMarchaMetrosMinuto,
            LocalTime horaSalidaEstimada,
            LocalTime horaEncierroLimite,
            LocalTime horaOficialPaso
    ) {
    }

    public record Response(
            Long id,
            String nombre,
            Integer numNazarenos,
            Double longitudCortejoMetros,
            Double velocidadMarchaMetrosMinuto,
            LocalTime horaSalidaEstimada,
            LocalTime horaEncierroLimite,
            LocalTime horaOficialPaso,
            Long escenarioId
    ) {
    }
}
