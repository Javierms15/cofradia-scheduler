package com.cofradias.web;

import com.cofradias.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EscenarioCofradiaRecorridoFlowIT extends AbstractIntegrationTest {

    private long crearEscenario(UsuarioRegistrado usuario, String nombre) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/usuarios/" + usuario.id() + "/escenarios")
                        .header("Authorization", bearer(usuario.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","fecha":"2027-03-14"}
                                """.formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long crearCofradia(UsuarioRegistrado usuario, long escenarioId, String nombre) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/escenarios/" + escenarioId + "/cofradias")
                        .header("Authorization", bearer(usuario.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","numNazarenos":500,"longitudCortejoMetros":300,"velocidadMarchaMetrosMinuto":35,
                                 "horaSalidaEstimada":"16:00:00","horaEncierroLimite":"23:00:00","horaOficialPaso":"18:00:00"}
                                """.formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    // ---- flujo feliz completo ----

    @Test
    void flujoCompletoUsuarioEscenarioCofradiaRecorrido() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("flujo"), "clave12345");

        long escenarioId = crearEscenario(javier, "Domingo de Ramos");
        long cofradiaId = crearCofradia(javier, escenarioId, "Pollinica");

        mockMvc.perform(post("/api/cofradias/" + cofradiaId + "/recorridos")
                        .header("Authorization", bearer(javier.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"OFICIAL","puntos":[{"lat":36.72101,"lon":-4.42134},{"lat":36.71938,"lon":-4.42010}],"distanciaMetros":210.5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("OFICIAL"))
                .andExpect(jsonPath("$.puntos", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.puntos[0].lat").value(36.72101))
                .andExpect(jsonPath("$.puntos[0].lon").value(-4.42134));

        mockMvc.perform(get("/api/cofradias/" + cofradiaId + "/recorridos")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].distanciaMetros").value(210.5));

        mockMvc.perform(get("/api/escenarios/" + escenarioId + "/cofradias")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Pollinica"));

        mockMvc.perform(get("/api/usuarios/" + javier.id() + "/escenarios")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Domingo de Ramos"));

        mockMvc.perform(get("/api/escenarios/" + escenarioId)
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(javier.id()));
    }

    @Test
    void obtenerUnaCofradiaPorIdDevuelveSusDatos() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("cofradia-get"), "clave12345");
        long escenarioId = crearEscenario(javier, "Con cofradia");
        long cofradiaId = crearCofradia(javier, escenarioId, "Pollinica");

        mockMvc.perform(get("/api/cofradias/" + cofradiaId)
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cofradiaId))
                .andExpect(jsonPath("$.nombre").value("Pollinica"))
                .andExpect(jsonPath("$.escenarioId").value(escenarioId));
    }

    @Test
    void obtenerUnaCofradiaInexistenteDevuelve404() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("cofradia-404"), "clave12345");

        mockMvc.perform(get("/api/cofradias/999999")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerUnaCofradiaDeOtroUsuarioDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("cofradia-403"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("cofradia-403m"), "otraclave1");
        long escenarioId = crearEscenario(javier, "Privado");
        long cofradiaId = crearCofradia(javier, escenarioId, "Privada");

        mockMvc.perform(get("/api/cofradias/" + cofradiaId)
                        .header("Authorization", bearer(maria.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unRecorridoConMenosDeDosPuntosDevuelve400Limpio() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("validacion"), "clave12345");
        long escenarioId = crearEscenario(javier, "Test");
        long cofradiaId = crearCofradia(javier, escenarioId, "Test");

        mockMvc.perform(post("/api/cofradias/" + cofradiaId + "/recorridos")
                        .header("Authorization", bearer(javier.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"IDA","puntos":[{"lat":36.72,"lon":-4.42}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.puntos").exists());
    }

    // ---- 404: recurso padre inexistente ----

    @Test
    void crearCofradiaEnEscenarioInexistenteDevuelve404() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("404esc"), "clave12345");

        mockMvc.perform(post("/api/escenarios/999999/cofradias")
                        .header("Authorization", bearer(javier.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"X","numNazarenos":100,"longitudCortejoMetros":100,"velocidadMarchaMetrosMinuto":30}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearRecorridoEnCofradiaInexistenteDevuelve404() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("404cof"), "clave12345");

        mockMvc.perform(post("/api/cofradias/999999/recorridos")
                        .header("Authorization", bearer(javier.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"OFICIAL","puntos":[{"lat":36.72,"lon":-4.42},{"lat":36.73,"lon":-4.43}]}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerEscenarioInexistenteDevuelve404() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("404esc2"), "clave12345");

        mockMvc.perform(get("/api/escenarios/999999")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isNotFound());
    }

    // ---- 403: matriz de autorizacion entre usuarios ----

    @Test
    void crearEscenarioParaOtroUsuarioDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("javier403"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("maria403"), "otraclave1");

        mockMvc.perform(post("/api/usuarios/" + javier.id() + "/escenarios")
                        .header("Authorization", bearer(maria.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Intruso"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarEscenariosDeOtroUsuarioDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("javier403b"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("maria403b"), "otraclave1");

        mockMvc.perform(get("/api/usuarios/" + javier.id() + "/escenarios")
                        .header("Authorization", bearer(maria.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerEscenarioDeOtroUsuarioDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("javier403c"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("maria403c"), "otraclave1");
        long escenarioId = crearEscenario(javier, "Privado");

        mockMvc.perform(get("/api/escenarios/" + escenarioId)
                        .header("Authorization", bearer(maria.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearCofradiaEnEscenarioAjenoDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("javier403d"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("maria403d"), "otraclave1");
        long escenarioId = crearEscenario(javier, "Privado");

        mockMvc.perform(post("/api/escenarios/" + escenarioId + "/cofradias")
                        .header("Authorization", bearer(maria.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Intrusa","numNazarenos":100,"longitudCortejoMetros":100,"velocidadMarchaMetrosMinuto":30}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarCofradiasDeEscenarioAjenoDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("javier403e"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("maria403e"), "otraclave1");
        long escenarioId = crearEscenario(javier, "Privado");

        mockMvc.perform(get("/api/escenarios/" + escenarioId + "/cofradias")
                        .header("Authorization", bearer(maria.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearRecorridoEnCofradiaAjenaDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("javier403f"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("maria403f"), "otraclave1");
        long escenarioId = crearEscenario(javier, "Privado");
        long cofradiaId = crearCofradia(javier, escenarioId, "Privada");

        mockMvc.perform(post("/api/cofradias/" + cofradiaId + "/recorridos")
                        .header("Authorization", bearer(maria.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"OFICIAL","puntos":[{"lat":36.72,"lon":-4.42},{"lat":36.73,"lon":-4.43}]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarRecorridosDeCofradiaAjenaDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("javier403g"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("maria403g"), "otraclave1");
        long escenarioId = crearEscenario(javier, "Privado");
        long cofradiaId = crearCofradia(javier, escenarioId, "Privada");

        mockMvc.perform(get("/api/cofradias/" + cofradiaId + "/recorridos")
                        .header("Authorization", bearer(maria.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestionarEscenariosDeUnUsuarioIdInexistenteDevuelve403NoInfoLeak() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("noleak"), "clave12345");

        mockMvc.perform(get("/api/usuarios/999999/escenarios")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isForbidden());
    }
}
