package com.cofradias.web;

import com.cofradias.dto.ResultadoDtos;
import com.cofradias.model.Escenario;
import com.cofradias.model.ResultadoOptimizacion;
import com.cofradias.model.Usuario;
import com.cofradias.optimization.OptimizacionService;
import com.cofradias.repository.EscenarioRepository;
import com.cofradias.repository.ResultadoOptimizacionRepository;
import com.cofradias.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OptimizacionController {

    private final EscenarioRepository escenarioRepository;
    private final ResultadoOptimizacionRepository resultadoOptimizacionRepository;
    private final OptimizacionService optimizacionService;
    private final CurrentUserProvider currentUserProvider;

    public OptimizacionController(EscenarioRepository escenarioRepository,
                                   ResultadoOptimizacionRepository resultadoOptimizacionRepository,
                                   OptimizacionService optimizacionService,
                                   CurrentUserProvider currentUserProvider) {
        this.escenarioRepository = escenarioRepository;
        this.resultadoOptimizacionRepository = resultadoOptimizacionRepository;
        this.optimizacionService = optimizacionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/escenarios/{escenarioId}/optimizar")
    public ResultadoDtos.Response optimizar(@PathVariable Long escenarioId, Authentication authentication) {
        escenarioDelPropietario(escenarioId, authentication);
        ResultadoOptimizacion resultado = optimizacionService.optimizar(escenarioId);
        return ResultadoDtos.Response.from(resultado);
    }

    @GetMapping("/escenarios/{escenarioId}/resultados")
    public List<ResultadoDtos.Summary> listar(@PathVariable Long escenarioId, Authentication authentication) {
        escenarioDelPropietario(escenarioId, authentication);
        return resultadoOptimizacionRepository.findByEscenarioId(escenarioId).stream()
                .map(ResultadoDtos.Summary::from)
                .toList();
    }

    @GetMapping("/resultados/{id}")
    public ResultadoDtos.Response obtener(@PathVariable Long id, Authentication authentication) {
        ResultadoOptimizacion resultado = resultadoOptimizacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado no encontrado"));

        Usuario usuarioActual = currentUserProvider.resolve(authentication);
        if (!resultado.getEscenario().getUsuario().getId().equals(usuarioActual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes consultar resultados de otro usuario");
        }

        return ResultadoDtos.Response.from(resultado);
    }

    private Escenario escenarioDelPropietario(Long escenarioId, Authentication authentication) {
        Escenario escenario = escenarioRepository.findById(escenarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escenario no encontrado"));

        Usuario usuarioActual = currentUserProvider.resolve(authentication);
        if (!escenario.getUsuario().getId().equals(usuarioActual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar el escenario de otro usuario");
        }

        return escenario;
    }
}
