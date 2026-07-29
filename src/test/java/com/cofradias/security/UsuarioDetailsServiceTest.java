package com.cofradias.security;

import com.cofradias.model.Usuario;
import com.cofradias.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void cargaUnUsuarioExistentePorEmail() {
        usuarioDetailsService = new UsuarioDetailsService(usuarioRepository);
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombre("Javier")
                .email("javier@example.com")
                .passwordHash("hash-bcrypt")
                .build();
        when(usuarioRepository.findByEmail("javier@example.com")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = usuarioDetailsService.loadUserByUsername("javier@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("javier@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    @Test
    void lanzaExcepcionSiElUsuarioNoExiste() {
        usuarioDetailsService = new UsuarioDetailsService(usuarioRepository);
        when(usuarioRepository.findByEmail("fantasma@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioDetailsService.loadUserByUsername("fantasma@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
