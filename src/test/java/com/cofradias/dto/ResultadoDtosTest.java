package com.cofradias.dto;

import com.cofradias.model.Cofradia;
import com.cofradias.model.Escenario;
import com.cofradias.model.ResultadoCofradia;
import com.cofradias.model.ResultadoOptimizacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadoDtosTest {

    @Test
    void construyeElResultadoDeUnaCofradiaAPartirDeLaEntidad() {
        Cofradia cofradia = Cofradia.builder()
                .id(1L)
                .nombre("Pollinica")
                .horaOficialPaso(LocalTime.of(18, 15))
                .build();
        ResultadoCofradia resultadoCofradia = ResultadoCofradia.builder()
                .cofradia(cofradia)
                .ordenAsignado(1)
                .horaEntradaCalculada(LocalTime.of(18, 0))
                .horaSalidaCalculada(LocalTime.of(18, 6))
                .retrasoMinutos(0)
                .build();

        ResultadoDtos.CofradiaResultado dto = ResultadoDtos.CofradiaResultado.from(resultadoCofradia);

        assertThat(dto.cofradiaId()).isEqualTo(1L);
        assertThat(dto.nombreCofradia()).isEqualTo("Pollinica");
        assertThat(dto.ordenAsignado()).isEqualTo(1);
        assertThat(dto.horaEntradaCalculada()).isEqualTo(LocalTime.of(18, 0));
        assertThat(dto.horaSalidaCalculada()).isEqualTo(LocalTime.of(18, 6));
        assertThat(dto.horaOficialPaso()).isEqualTo(LocalTime.of(18, 15));
        assertThat(dto.retrasoMinutos()).isEqualTo(0);
    }

    @Test
    void elResponseOrdenaLasCofradiasPorOrdenAsignadoIndependientementeDelOrdenDeInsercion() {
        Escenario escenario = Escenario.builder().id(9L).build();
        Cofradia cofradiaA = Cofradia.builder().id(1L).nombre("A").build();
        Cofradia cofradiaB = Cofradia.builder().id(2L).nombre("B").build();

        ResultadoOptimizacion resultado = ResultadoOptimizacion.builder()
                .id(3L)
                .escenario(escenario)
                .fechaCalculo(LocalDateTime.of(2026, 3, 14, 12, 0))
                .retrasoTotalMinutos(41)
                .retrasoMaximoMinutos(29)
                .build();

        ResultadoCofradia segundo = ResultadoCofradia.builder().cofradia(cofradiaB).ordenAsignado(2).build();
        ResultadoCofradia primero = ResultadoCofradia.builder().cofradia(cofradiaA).ordenAsignado(1).build();
        resultado.setResultadosCofradias(List.of(segundo, primero));

        ResultadoDtos.Response response = ResultadoDtos.Response.from(resultado);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.escenarioId()).isEqualTo(9L);
        assertThat(response.retrasoTotalMinutos()).isEqualTo(41);
        assertThat(response.retrasoMaximoMinutos()).isEqualTo(29);
        assertThat(response.cofradias()).extracting(ResultadoDtos.CofradiaResultado::nombreCofradia)
                .containsExactly("A", "B");
    }

    @Test
    void elSummaryNoIncluyeElDetallePorCofradia() {
        Escenario escenario = Escenario.builder().id(9L).build();
        ResultadoOptimizacion resultado = ResultadoOptimizacion.builder()
                .id(3L)
                .escenario(escenario)
                .fechaCalculo(LocalDateTime.of(2026, 3, 14, 12, 0))
                .retrasoTotalMinutos(41)
                .retrasoMaximoMinutos(29)
                .build();

        ResultadoDtos.Summary summary = ResultadoDtos.Summary.from(resultado);

        assertThat(summary.id()).isEqualTo(3L);
        assertThat(summary.retrasoTotalMinutos()).isEqualTo(41);
        assertThat(summary.retrasoMaximoMinutos()).isEqualTo(29);
        assertThat(summary.fechaCalculo()).isEqualTo(LocalDateTime.of(2026, 3, 14, 12, 0));
    }
}
