package com.cofradias.dto;

import com.cofradias.model.TipoTramo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.List;

public class RecorridoDtos {

    public record Punto(
            @NotNull Double lat,
            @NotNull Double lon
    ) {
        public Coordinate toCoordinate() {
            return new Coordinate(lon, lat);
        }

        public static Punto from(Coordinate coordinate) {
            return new Punto(coordinate.getY(), coordinate.getX());
        }
    }

    public record CreateRequest(
            @NotNull TipoTramo tipo,
            @NotNull @Size(min = 2, message = "Un recorrido necesita al menos 2 puntos") List<@Valid Punto> puntos,
            @Positive Double distanciaMetros
    ) {
    }

    public record Response(
            Long id,
            TipoTramo tipo,
            List<Punto> puntos,
            Double distanciaMetros,
            Long cofradiaId
    ) {
        public static Response from(com.cofradias.model.Recorrido recorrido) {
            LineString geometria = recorrido.getGeometria();
            List<Punto> puntos = geometria == null
                    ? List.of()
                    : java.util.Arrays.stream(geometria.getCoordinates()).map(Punto::from).toList();

            return new Response(
                    recorrido.getId(),
                    recorrido.getTipo(),
                    puntos,
                    recorrido.getDistanciaMetros(),
                    recorrido.getCofradia().getId()
            );
        }
    }
}
