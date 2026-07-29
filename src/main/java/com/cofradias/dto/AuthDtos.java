package com.cofradias.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record TokenResponse(
            String token,
            UsuarioDtos.Response usuario
    ) {
    }
}
