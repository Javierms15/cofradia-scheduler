package com.cofradias.web;

import com.cofradias.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OptimizacionControllerIT extends AbstractIntegrationTest {

    private long crearEscenario(UsuarioRegistrado usuario, String nombre) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/usuarios/" + usuario.id() + "/escenarios")
                        .header("Authorization", bearer(usuario.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","fecha":"2027-03-14"}
                                """.formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long crearCofradiaCompleta(UsuarioRegistrado usuario, long escenarioId, String nombre,
                                        String horaSalida, String horaEncierro, String horaOficial,
                                        double velocidad, double longitudCortejo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/escenarios/" + escenarioId + "/cofradias")
                        .header("Authorization", bearer(usuario.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","numNazarenos":500,"longitudCortejoMetros":%s,"velocidadMarchaMetrosMinuto":%s,
                                 "horaSalidaEstimada":"%s","horaEncierroLimite":"%s","horaOficialPaso":"%s"}
                                """.formatted(nombre, longitudCortejo, velocidad, horaSalida, horaEncierro, horaOficial)))
                .andExpect(status().isCreated())
                .andReturn();
        long cofradiaId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(post("/api/cofradias/" + cofradiaId + "/recorridos")
                        .header("Authorization", bearer(usuario.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"OFICIAL","puntos":[{"lat":36.72101,"lon":-4.42134},{"lat":36.71938,"lon":-4.42010}],"distanciaMetros":210}
                                """))
                .andExpect(status().isCreated());

        return cofradiaId;
    }

    @Test
    void optimizarEscenarioConMargenAmplioDaRetrasoTotalCero() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("opt-margen"), "clave12345");
        long escenarioId = crearEscenario(javier, "Con margen");

        crearCofradiaCompleta(javier, escenarioId, "Pollinica", "16:30:00", "22:00:00", "18:15:00", 40, 450.5);
        crearCofradiaCompleta(javier, escenarioId, "Amor", "16:00:00", "23:00:00", "18:10:00", 35, 300);
        crearCofradiaCompleta(javier, escenarioId, "Esperanza", "17:00:00", "23:30:00", "18:12:00", 30, 600);

        mockMvc.perform(post("/api/escenarios/" + escenarioId + "/optimizar")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrasoTotalMinutos").value(0))
                .andExpect(jsonPath("$.retrasoMaximoMinutos").value(0))
                .andExpect(jsonPath("$.cofradias", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.cofradias[0].ordenAsignado").value(1))
                .andExpect(jsonPath("$.cofradias[1].ordenAsignado").value(2))
                .andExpect(jsonPath("$.cofradias[2].ordenAsignado").value(3));
    }

    @Test
    void optimizarEscenarioConConflictoGeneraCascadaDeRetrasos() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("opt-cascada"), "clave12345");
        long escenarioId = crearEscenario(javier, "Con conflicto");

        for (String nombre : new String[]{"A", "B", "C"}) {
            crearCofradiaCompleta(javier, escenarioId, nombre, "18:00:00", "23:00:00", "18:05:00", 40, 450);
        }

        MvcResult result = mockMvc.perform(post("/api/escenarios/" + escenarioId + "/optimizar")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrasoTotalMinutos").value(41))
                .andExpect(jsonPath("$.retrasoMaximoMinutos").value(29))
                .andReturn();

        java.util.List<Integer> retrasos = JsonPath.read(result.getResponse().getContentAsString(), "$.cofradias[*].retrasoMinutos");
        assertThat(retrasos).containsExactly(0, 12, 29);
    }

    @Test
    void optimizarSinDatosSuficientesDevuelve422() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("opt-422"), "clave12345");
        long escenarioId = crearEscenario(javier, "Incompleto");

        // cofradia sin recorrido OFICIAL ni horas
        mockMvc.perform(post("/api/escenarios/" + escenarioId + "/cofradias")
                        .header("Authorization", bearer(javier.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Incompleta","numNazarenos":100,"longitudCortejoMetros":100,"velocidadMarchaMetrosMinuto":30}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/escenarios/" + escenarioId + "/optimizar")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Incompleta")));
    }

    @Test
    void optimizarUnEscenarioVacioDevuelve422() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("opt-vacio"), "clave12345");
        long escenarioId = crearEscenario(javier, "Vacio");

        mockMvc.perform(post("/api/escenarios/" + escenarioId + "/optimizar")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El escenario no tiene cofradias"));
    }

    @Test
    void optimizarEscenarioInexistenteDevuelve404() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("opt-404"), "clave12345");

        mockMvc.perform(post("/api/escenarios/999999/optimizar")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void optimizarElEscenarioDeOtroUsuarioDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("opt-javier403"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("opt-maria403"), "otraclave1");
        long escenarioId = crearEscenario(javier, "Privado");

        mockMvc.perform(post("/api/escenarios/" + escenarioId + "/optimizar")
                        .header("Authorization", bearer(maria.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarYObtenerResultadosDeOptimizacion() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("opt-listar"), "clave12345");
        long escenarioId = crearEscenario(javier, "Con resultados");
        crearCofradiaCompleta(javier, escenarioId, "Unica", "16:00:00", "23:00:00", "18:00:00", 40, 300);

        mockMvc.perform(post("/api/escenarios/" + escenarioId + "/optimizar")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk());

        MvcResult listado = mockMvc.perform(get("/api/escenarios/" + escenarioId + "/resultados")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andReturn();

        long resultadoId = ((Number) JsonPath.read(listado.getResponse().getContentAsString(), "$[0].id")).longValue();

        mockMvc.perform(get("/api/resultados/" + resultadoId)
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resultadoId))
                .andExpect(jsonPath("$.cofradias[0].nombreCofradia").value("Unica"));
    }

    @Test
    void obtenerUnResultadoDeOtroUsuarioDevuelve403() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("opt-res403"), "clave12345");
        UsuarioRegistrado maria = registrar("Maria", emailUnico("opt-res403m"), "otraclave1");
        long escenarioId = crearEscenario(javier, "Con resultado privado");
        crearCofradiaCompleta(javier, escenarioId, "Unica", "16:00:00", "23:00:00", "18:00:00", 40, 300);

        MvcResult optim = mockMvc.perform(post("/api/escenarios/" + escenarioId + "/optimizar")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isOk())
                .andReturn();
        long resultadoId = ((Number) JsonPath.read(optim.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/resultados/" + resultadoId)
                        .header("Authorization", bearer(maria.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerUnResultadoInexistenteDevuelve404() throws Exception {
        UsuarioRegistrado javier = registrar("Javier", emailUnico("opt-res404"), "clave12345");

        mockMvc.perform(get("/api/resultados/999999")
                        .header("Authorization", bearer(javier.token())))
                .andExpect(status().isNotFound());
    }
}
