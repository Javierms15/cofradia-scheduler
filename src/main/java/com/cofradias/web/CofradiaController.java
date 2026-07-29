package com.cofradias.web;

import com.cofradias.dto.CofradiaDtos;
import com.cofradias.model.Cofradia;
import com.cofradias.model.Escenario;
import com.cofradias.model.Usuario;
import com.cofradias.repository.CofradiaRepository;
import com.cofradias.repository.EscenarioRepository;
import com.cofradias.security.CurrentUserProvider;
import jakarta.validation.Valid;
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
public class CofradiaController {

    private final CofradiaRepository cofradiaRepository;
    private final EscenarioRepository escenarioRepository;
    private final CurrentUserProvider currentUserProvider;

    public CofradiaController(CofradiaRepository cofradiaRepository, EscenarioRepository escenarioRepository,
                               CurrentUserProvider currentUserProvider) {
        this.cofradiaRepository = cofradiaRepository;
        this.escenarioRepository = escenarioRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/escenarios/{escenarioId}/cofradias")
    @ResponseStatus(HttpStatus.CREATED)
    public CofradiaDtos.Response crear(@PathVariable Long escenarioId, @Valid @RequestBody CofradiaDtos.CreateRequest request,
                                        Authentication authentication) {
        Escenario escenario = escenarioDelPropietario(escenarioId, authentication);

        Cofradia cofradia = Cofradia.builder()
                .escenario(escenario)
                .nombre(request.nombre())
                .numNazarenos(request.numNazarenos())
                .longitudCortejoMetros(request.longitudCortejoMetros())
                .velocidadMarchaMetrosMinuto(request.velocidadMarchaMetrosMinuto())
                .horaSalidaEstimada(request.horaSalidaEstimada())
                .horaEncierroLimite(request.horaEncierroLimite())
                .horaOficialPaso(request.horaOficialPaso())
                .build();

        Cofradia guardada = cofradiaRepository.save(cofradia);
        return toResponse(guardada);
    }

    @GetMapping("/escenarios/{escenarioId}/cofradias")
    public List<CofradiaDtos.Response> listarPorEscenario(@PathVariable Long escenarioId, Authentication authentication) {
        escenarioDelPropietario(escenarioId, authentication);
        return cofradiaRepository.findByEscenarioId(escenarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/cofradias/{id}")
    public CofradiaDtos.Response obtener(@PathVariable Long id, Authentication authentication) {
        Cofradia cofradia = cofradiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cofradia no encontrada"));

        Usuario usuarioActual = currentUserProvider.resolve(authentication);
        if (!cofradia.getEscenario().getUsuario().getId().equals(usuarioActual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes consultar cofradias de otro usuario");
        }

        return toResponse(cofradia);
    }

    private Escenario escenarioDelPropietario(Long escenarioId, Authentication authentication) {
        Escenario escenario = escenarioRepository.findById(escenarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escenario no encontrado"));

        Usuario usuarioActual = currentUserProvider.resolve(authentication);
        if (!escenario.getUsuario().getId().equals(usuarioActual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar cofradias de otro usuario");
        }

        return escenario;
    }

    private CofradiaDtos.Response toResponse(Cofradia cofradia) {
        return new CofradiaDtos.Response(
                cofradia.getId(),
                cofradia.getNombre(),
                cofradia.getNumNazarenos(),
                cofradia.getLongitudCortejoMetros(),
                cofradia.getVelocidadMarchaMetrosMinuto(),
                cofradia.getHoraSalidaEstimada(),
                cofradia.getHoraEncierroLimite(),
                cofradia.getHoraOficialPaso(),
                cofradia.getEscenario().getId()
        );
    }
}
