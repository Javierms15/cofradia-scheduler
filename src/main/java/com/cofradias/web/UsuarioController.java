package com.cofradias.web;

import com.cofradias.dto.UsuarioDtos;
import com.cofradias.model.Usuario;
import com.cofradias.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final CurrentUserProvider currentUserProvider;

    public UsuarioController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/{id}")
    public UsuarioDtos.Response obtener(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioActual = currentUserProvider.resolve(authentication);
        if (!usuarioActual.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes consultar el perfil de otro usuario");
        }
        return UsuarioDtos.Response.from(usuarioActual);
    }
}
