package com.cofradias.dto;

import com.cofradias.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UsuarioDtos {

    public record CreateRequest(
            @NotBlank String nombre,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password
    ) {
    }

    public record Response(
            Long id,
            String nombre,
            String email,
            LocalDateTime fechaCreacion
    ) {
        public static Response from(Usuario usuario) {
            return new Response(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getFechaCreacion());
        }
    }
}
