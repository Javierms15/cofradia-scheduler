package com.cofradias.dto;

import com.cofradias.model.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioDtosTest {

    @Test
    void construyeElResponseAPartirDeLaEntidad() {
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 1, 1, 10, 0);
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombre("Javier")
                .email("javier@example.com")
                .passwordHash("hash-no-expuesto")
                .fechaCreacion(fechaCreacion)
                .build();

        UsuarioDtos.Response response = UsuarioDtos.Response.from(usuario);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Javier");
        assertThat(response.email()).isEqualTo("javier@example.com");
        assertThat(response.fechaCreacion()).isEqualTo(fechaCreacion);
    }
}
