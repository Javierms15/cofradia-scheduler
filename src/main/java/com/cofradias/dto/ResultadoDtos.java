package com.cofradias.dto;

import com.cofradias.model.ResultadoCofradia;
import com.cofradias.model.ResultadoOptimizacion;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

public class ResultadoDtos {

    public record CofradiaResultado(
            Long cofradiaId,
            String nombreCofradia,
            Integer ordenAsignado,
            LocalTime horaEntradaCalculada,
            LocalTime horaSalidaCalculada,
            LocalTime horaOficialPaso,
            Integer retrasoMinutos
    ) {
        public static CofradiaResultado from(ResultadoCofradia rc) {
            return new CofradiaResultado(
                    rc.getCofradia().getId(),
                    rc.getCofradia().getNombre(),
                    rc.getOrdenAsignado(),
                    rc.getHoraEntradaCalculada(),
                    rc.getHoraSalidaCalculada(),
                    rc.getCofradia().getHoraOficialPaso(),
                    rc.getRetrasoMinutos()
            );
        }
    }

    public record Response(
            Long id,
            LocalDateTime fechaCalculo,
            Integer retrasoTotalMinutos,
            Integer retrasoMaximoMinutos,
            Long escenarioId,
            List<CofradiaResultado> cofradias
    ) {
        public static Response from(ResultadoOptimizacion r) {
            List<CofradiaResultado> cofradias = r.getResultadosCofradias().stream()
                    .sorted(Comparator.comparing(ResultadoCofradia::getOrdenAsignado))
                    .map(CofradiaResultado::from)
                    .toList();

            return new Response(
                    r.getId(),
                    r.getFechaCalculo(),
                    r.getRetrasoTotalMinutos(),
                    r.getRetrasoMaximoMinutos(),
                    r.getEscenario().getId(),
                    cofradias
            );
        }
    }

    public record Summary(
            Long id,
            LocalDateTime fechaCalculo,
            Integer retrasoTotalMinutos,
            Integer retrasoMaximoMinutos
    ) {
        public static Summary from(ResultadoOptimizacion r) {
            return new Summary(r.getId(), r.getFechaCalculo(), r.getRetrasoTotalMinutos(), r.getRetrasoMaximoMinutos());
        }
    }
}
