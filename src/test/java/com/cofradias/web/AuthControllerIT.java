package com.cofradias.web;

import com.cofradias.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends AbstractIntegrationTest {

    @Test
    void registrarUnUsuarioNuevoDevuelveTokenYDatosDelUsuario() throws Exception {
        String email = emailUnico("javier");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Javier","email":"%s","password":"clave12345"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.id").isNumber())
                .andExpect(jsonPath("$.usuario.nombre").value("Javier"))
                .andExpect(jsonPath("$.usuario.email").value(email));
    }

    @Test
    void registrarConEmailYaExistenteDevuelve409() throws Exception {
        String email = emailUnico("duplicado");
        registrar("Javier", email, "clave12345");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Otro","email":"%s","password":"otraclave1"}
                                """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un usuario con ese email"));
    }

    @Test
    void registrarConEmailInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Javier","email":"no-es-un-email","password":"clave12345"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists());
    }

    @Test
    void registrarConPasswordCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Javier","email":"%s","password":"corta"}
                                """.formatted(emailUnico("javier2"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void registrarConNombreVacioDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","email":"%s","password":"clave12345"}
                                """.formatted(emailUnico("javier3"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void loginConCredencialesValidasDevuelveToken() throws Exception {
        String email = emailUnico("login-ok");
        registrar("Javier", email, "clave12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"clave12345"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value(email));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        String email = emailUnico("login-mal");
        registrar("Javier", email, "clave12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"noEsLaClave"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales invalidas"));
    }

    @Test
    void loginConEmailInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"clave12345"}
                                """.formatted(emailUnico("fantasma"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accederAUnEndpointProtegidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accederAUnEndpointProtegidoConTokenManipuladoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios/1").header("Authorization", "Bearer token.manipulado.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accederAUnEndpointProtegidoConTokenValidoFunciona() throws Exception {
        String email = emailUnico("protegido");
        UsuarioRegistrado usuario = registrar("Javier", email, "clave12345");

        mockMvc.perform(get("/api/usuarios/" + usuario.id()).header("Authorization", bearer(usuario.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void consultarElPerfilDeOtroUsuarioDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("javier-perfil"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("maria-perfil"), "otraclave1");

        mockMvc.perform(get("/api/usuarios/" + javier.id()).header("Authorization", bearer(maria.token())))
                .andExpect(status().isForbidden());
    }
}
