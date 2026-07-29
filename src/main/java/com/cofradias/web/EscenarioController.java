package com.cofradias.web;

import com.cofradias.dto.EscenarioDtos;
import com.cofradias.model.Escenario;
import com.cofradias.model.Usuario;
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
public class EscenarioController {

    private final EscenarioRepository escenarioRepository;
    private final CurrentUserProvider currentUserProvider;

    public EscenarioController(EscenarioRepository escenarioRepository, CurrentUserProvider currentUserProvider) {
        this.escenarioRepository = escenarioRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/usuarios/{usuarioId}/escenarios")
    @ResponseStatus(HttpStatus.CREATED)
    public EscenarioDtos.Response crear(@PathVariable Long usuarioId, @Valid @RequestBody EscenarioDtos.CreateRequest request,
                                         Authentication authentication) {
        Usuario usuarioActual = requireMismo(usuarioId, authentication);

        Escenario escenario = Escenario.builder()
                .usuario(usuarioActual)
                .nombre(request.nombre())
                .fecha(request.fecha())
                .build();

        Escenario guardado = escenarioRepository.save(escenario);
        return toResponse(guardado);
    }

    @GetMapping("/usuarios/{usuarioId}/escenarios")
    public List<EscenarioDtos.Response> listarPorUsuario(@PathVariable Long usuarioId, Authentication authentication) {
        requireMismo(usuarioId, authentication);
        return escenarioRepository.findByUsuarioId(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/escenarios/{id}")
    public EscenarioDtos.Response obtener(@PathVariable Long id, Authentication authentication) {
        Escenario escenario = escenarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escenario no encontrado"));

        Usuario usuarioActual = currentUserProvider.resolve(authentication);
        if (!escenario.getUsuario().getId().equals(usuarioActual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes consultar escenarios de otro usuario");
        }

        return toResponse(escenario);
    }

    private Usuario requireMismo(Long usuarioId, Authentication authentication) {
        Usuario usuarioActual = currentUserProvider.resolve(authentication);
        if (!usuarioActual.getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar escenarios de otro usuario");
        }
        return usuarioActual;
    }

    private EscenarioDtos.Response toResponse(Escenario escenario) {
        return new EscenarioDtos.Response(
                escenario.getId(),
                escenario.getNombre(),
                escenario.getFecha(),
                escenario.getFechaCreacion(),
                escenario.getUsuario().getId()
        );
    }
}
