package com.cofradias.dto;

import com.cofradias.model.Cofradia;
import com.cofradias.model.Recorrido;
import com.cofradias.model.TipoTramo;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecorridoDtosTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    void unPuntoSeConvierteACoordenadaJtsConLonComoXYLatComoY() {
        RecorridoDtos.Punto punto = new RecorridoDtos.Punto(36.72101, -4.42134);

        Coordinate coordenada = punto.toCoordinate();

        assertThat(coordenada.getX()).isEqualTo(-4.42134);
        assertThat(coordenada.getY()).isEqualTo(36.72101);
    }

    @Test
    void unaCoordenadaJtsSeConvierteDeVueltaAlMismoPunto() {
        Coordinate coordenada = new Coordinate(-4.42134, 36.72101);

        RecorridoDtos.Punto punto = RecorridoDtos.Punto.from(coordenada);

        assertThat(punto.lat()).isEqualTo(36.72101);
        assertThat(punto.lon()).isEqualTo(-4.42134);
    }

    @Test
    void elResponseExtraeLosPuntosDeLaGeometriaEnElMismoOrden() {
        LineString geometria = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(-4.42134, 36.72101),
                new Coordinate(-4.42010, 36.71938)
        });
        Cofradia cofradia = Cofradia.builder().id(5L).nombre("Pollinica").build();
        Recorrido recorrido = Recorrido.builder()
                .id(1L)
                .cofradia(cofradia)
                .tipo(TipoTramo.OFICIAL)
                .geometria(geometria)
                .distanciaMetros(210.5)
                .build();

        RecorridoDtos.Response response = RecorridoDtos.Response.from(recorrido);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.tipo()).isEqualTo(TipoTramo.OFICIAL);
        assertThat(response.distanciaMetros()).isEqualTo(210.5);
        assertThat(response.cofradiaId()).isEqualTo(5L);
        assertThat(response.puntos()).containsExactly(
                new RecorridoDtos.Punto(36.72101, -4.42134),
                new RecorridoDtos.Punto(36.71938, -4.42010)
        );
    }

    @Test
    void elResponseDevuelveListaVaciaSiNoHayGeometria() {
        Cofradia cofradia = Cofradia.builder().id(5L).nombre("Pollinica").build();
        Recorrido recorrido = Recorrido.builder()
                .id(1L)
                .cofradia(cofradia)
                .tipo(TipoTramo.IDA)
                .geometria(null)
                .distanciaMetros(50.0)
                .build();

        RecorridoDtos.Response response = RecorridoDtos.Response.from(recorrido);

        assertThat(response.puntos()).isEqualTo(List.of());
    }
}
