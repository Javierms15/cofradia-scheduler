package com.cofradias.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para tests de integracion. Ejercita la app completa (seguridad, JPA/Hibernate Spatial,
 * controladores) igual que hicimos manualmente con curl durante el desarrollo, contra el mismo
 * PostgreSQL/PostGIS real que levanta el docker-compose.yml del proyecto (necesario porque el
 * tipo de columna geometry(LineString,4326) de Recorrido no existe en una base en memoria como H2).
 *
 * <p>Requisito para ejecutar estos tests: tener la base de datos arrancada con
 * {@code docker compose up -d} antes de lanzar {@code mvn test} (misma base que usa la app en
 * desarrollo, configurada en application.yml).
 *
 * <p>Nota: no se usa Testcontainers para levantar la base de forma efimera porque, en el momento
 * de escribir estos tests, la version de Docker Desktop instalada en esta maquina no es compatible
 * con el transporte de Testcontainers sobre named pipes de Windows (fallo al negociar con el
 * Docker Engine API). Es un candidato natural para migrar en cuanto se actualice esa dependencia.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected record UsuarioRegistrado(Long id, String email, String token) {
    }

    protected UsuarioRegistrado registrar(String nombre, String email, String password) throws Exception {
        String body = """
                {"nombre":"%s","email":"%s","password":"%s"}
                """.formatted(nombre, email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(json, "$.usuario.id")).longValue();
        String token = JsonPath.read(json, "$.token");
        return new UsuarioRegistrado(id, email, token);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    /**
     * Genera un email unico por test (usando nanoTime) para que cada metodo de test sea
     * independiente sin necesidad de limpiar la base de datos entre ejecuciones.
     */
    protected String emailUnico(String prefijo) {
        return prefijo + "-" + System.nanoTime() + "@example.com";
    }
}
