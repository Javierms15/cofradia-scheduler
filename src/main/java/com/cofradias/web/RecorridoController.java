package com.cofradias.web;

import com.cofradias.dto.RecorridoDtos;
import com.cofradias.model.Cofradia;
import com.cofradias.model.Recorrido;
import com.cofradias.model.Usuario;
import com.cofradias.repository.CofradiaRepository;
import com.cofradias.repository.RecorridoRepository;
import com.cofradias.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RecorridoController {

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private final RecorridoRepository recorridoRepository;
    private final CofradiaRepository cofradiaRepository;
    private final CurrentUserProvider currentUserProvider;

    public RecorridoController(RecorridoRepository recorridoRepository, CofradiaRepository cofradiaRepository,
                                CurrentUserProvider currentUserProvider) {
        this.recorridoRepository = recorridoRepository;
        this.cofradiaRepository = cofradiaRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/cofradias/{cofradiaId}/recorridos")
    @ResponseStatus(HttpStatus.CREATED)
    public RecorridoDtos.Response crear(@PathVariable Long cofradiaId, @Valid @RequestBody RecorridoDtos.CreateRequest request,
                                         Authentication authentication) {
        Cofradia cofradia = cofradiaDelPropietario(cofradiaId, authentication);

        Coordinate[] coordenadas = request.puntos().stream()
                .map(RecorridoDtos.Punto::toCoordinate)
                .toArray(Coordinate[]::new);
        LineString geometria = GEOMETRY_FACTORY.createLineString(coordenadas);

        Recorrido recorrido = Recorrido.builder()
                .cofradia(cofradia)
                .tipo(request.tipo())
                .geometria(geometria)
                .distanciaMetros(request.distanciaMetros())
                .build();

        Recorrido guardado = recorridoRepository.save(recorrido);
        return RecorridoDtos.Response.from(guardado);
    }

    @GetMapping("/cofradias/{cofradiaId}/recorridos")
    public List<RecorridoDtos.Response> listarPorCofradia(@PathVariable Long cofradiaId, Authentication authentication) {
        cofradiaDelPropietario(cofradiaId, authentication);
        return recorridoRepository.findByCofradiaId(cofradiaId).stream()
                .map(RecorridoDtos.Response::from)
                .toList();
    }

    private Cofradia cofradiaDelPropietario(Long cofradiaId, Authentication authentication) {
        Cofradia cofradia = cofradiaRepository.findById(cofradiaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cofradia no encontrada"));

        Usuario usuarioActual = currentUserProvider.resolve(authentication);
        if (!cofradia.getEscenario().getUsuario().getId().equals(usuarioActual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar recorridos de otro usuario");
        }

        return cofradia;
    }
}
