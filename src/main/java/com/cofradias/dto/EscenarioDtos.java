package com.cofradias.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EscenarioDtos {

    public record CreateRequest(
            @NotBlank String nombre,
            LocalDate fecha
    ) {
    }

    public record Response(
            Long id,
            String nombre,
            LocalDate fecha,
            LocalDateTime fechaCreacion,
            Long usuarioId
    ) {
    }
}
