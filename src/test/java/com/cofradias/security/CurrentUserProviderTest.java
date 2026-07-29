package com.cofradias.security;

import com.cofradias.model.Usuario;
import com.cofradias.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Authentication authentication;

    private CurrentUserProvider currentUserProvider;

    @Test
    void resuelveElUsuarioAsociadoAlEmailDeLaAutenticacion() {
        currentUserProvider = new CurrentUserProvider(usuarioRepository);
        Usuario usuario = Usuario.builder().id(1L).nombre("Javier").email("javier@example.com").build();
        when(authentication.getName()).thenReturn("javier@example.com");
        when(usuarioRepository.findByEmail("javier@example.com")).thenReturn(Optional.of(usuario));

        Usuario resuelto = currentUserProvider.resolve(authentication);

        assertThat(resuelto).isEqualTo(usuario);
    }

    @Test
    void lanza401SiElEmailAutenticadoNoCorrespondeAUnUsuario() {
        currentUserProvider = new CurrentUserProvider(usuarioRepository);
        when(authentication.getName()).thenReturn("fantasma@example.com");
        when(usuarioRepository.findByEmail("fantasma@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUserProvider.resolve(authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
