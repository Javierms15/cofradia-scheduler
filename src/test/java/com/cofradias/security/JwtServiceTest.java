package com.cofradias.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "esto-es-un-secreto-de-pruebas-de-al-menos-32-bytes-para-hs256";

    private final JwtService jwtService = new JwtService(SECRET, 60_000L);

    @Test
    void generaUnTokenYExtraeElMismoSubject() {
        String token = jwtService.generateToken("javier@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractSubject(token)).isEqualTo("javier@example.com");
    }

    @Test
    void unTokenRecienGeneradoEsValidoParaSuPropioSubject() {
        String token = jwtService.generateToken("javier@example.com");

        assertThat(jwtService.isValid(token, "javier@example.com")).isTrue();
    }

    @Test
    void unTokenNoEsValidoParaOtroSubject() {
        String token = jwtService.generateToken("javier@example.com");

        assertThat(jwtService.isValid(token, "otro@example.com")).isFalse();
    }

    @Test
    void unTokenExpiradoNoEsValido() throws InterruptedException {
        JwtService servicioDeVidaCorta = new JwtService(SECRET, 1L);
        String token = servicioDeVidaCorta.generateToken("javier@example.com");

        Thread.sleep(20);

        assertThat(servicioDeVidaCorta.isValid(token, "javier@example.com")).isFalse();
    }

    @Test
    void unTokenManipuladoNoEsValidoYNoLanzaExcepcion() {
        String token = jwtService.generateToken("javier@example.com");
        String tokenManipulado = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.isValid(tokenManipulado, "javier@example.com")).isFalse();
    }

    @Test
    void unTokenConFormatoInvalidoNoEsValido() {
        assertThat(jwtService.isValid("esto-no-es-un-jwt", "javier@example.com")).isFalse();
    }

    @Test
    void unTokenFirmadoConOtroSecretoNoEsValido() {
        JwtService otroServicio = new JwtService("otro-secreto-completamente-distinto-de-32-bytes-o-mas", 60_000L);
        String token = otroServicio.generateToken("javier@example.com");

        assertThat(jwtService.isValid(token, "javier@example.com")).isFalse();
    }
}
