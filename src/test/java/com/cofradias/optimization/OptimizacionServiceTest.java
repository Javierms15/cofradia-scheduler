package com.cofradias.optimization;

import com.cofradias.model.Cofradia;
import com.cofradias.model.Escenario;
import com.cofradias.model.Recorrido;
import com.cofradias.model.ResultadoOptimizacion;
import com.cofradias.model.TipoTramo;
import com.cofradias.repository.EscenarioRepository;
import com.cofradias.repository.ResultadoOptimizacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptimizacionServiceTest {

    @Mock
    private EscenarioRepository escenarioRepository;

    @Mock
    private ResultadoOptimizacionRepository resultadoOptimizacionRepository;

    private OptimizacionService optimizacionService;

    @BeforeEach
    void setUp() {
        optimizacionService = new OptimizacionService(escenarioRepository, resultadoOptimizacionRepository);
    }

    private void permitirGuardado() {
        when(resultadoOptimizacionRepository.save(any(ResultadoOptimizacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Recorrido tramo(TipoTramo tipo, double distanciaMetros) {
        return Recorrido.builder().tipo(tipo).distanciaMetros(distanciaMetros).build();
    }

    private Cofradia.CofradiaBuilder cofradiaBase(Long id, String nombre) {
        return Cofradia.builder().id(id).nombre(nombre).recorridos(new ArrayList<>());
    }

    private Escenario escenarioCon(Cofradia... cofradias) {
        return Escenario.builder().id(1L).nombre("Test").cofradias(List.of(cofradias)).build();
    }

    // ---- validaciones previas ----

    @Test
    void lanza404SiElEscenarioNoExiste() {
        when(escenarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> optimizacionService.optimizar(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void lanza422SiElEscenarioNoTieneCofradias() {
        Escenario escenario = escenarioCon();
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));

        assertThatThrownBy(() -> optimizacionService.optimizar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no tiene cofradias");
    }

    @Test
    void lanza422SiFaltaLaVelocidadDeMarcha() {
        Cofradia c = cofradiaBase(1L, "Sin velocidad")
                .horaSalidaEstimada(LocalTime.of(16, 0))
                .horaEncierroLimite(LocalTime.of(23, 0))
                .horaOficialPaso(LocalTime.of(18, 0))
                .build();
        c.getRecorridos().add(tramo(TipoTramo.OFICIAL, 200));
        Escenario escenario = escenarioCon(c);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));

        assertThatThrownBy(() -> optimizacionService.optimizar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("velocidad de marcha");
    }

    @Test
    void lanza422SiFaltaLaHoraDeSalidaEstimada() {
        Cofradia c = cofradiaBase(1L, "Sin salida")
                .velocidadMarchaMetrosMinuto(40.0)
                .horaEncierroLimite(LocalTime.of(23, 0))
                .horaOficialPaso(LocalTime.of(18, 0))
                .build();
        c.getRecorridos().add(tramo(TipoTramo.OFICIAL, 200));
        Escenario escenario = escenarioCon(c);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));

        assertThatThrownBy(() -> optimizacionService.optimizar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("hora de salida estimada");
    }

    @Test
    void lanza422SiFaltaLaHoraDeEncierroLimite() {
        Cofradia c = cofradiaBase(1L, "Sin encierro")
                .velocidadMarchaMetrosMinuto(40.0)
                .horaSalidaEstimada(LocalTime.of(16, 0))
                .horaOficialPaso(LocalTime.of(18, 0))
                .build();
        c.getRecorridos().add(tramo(TipoTramo.OFICIAL, 200));
        Escenario escenario = escenarioCon(c);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));

        assertThatThrownBy(() -> optimizacionService.optimizar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("hora de encierro limite");
    }

    @Test
    void lanza422SiFaltaLaHoraOficialDePaso() {
        Cofradia c = cofradiaBase(1L, "Sin hora oficial")
                .velocidadMarchaMetrosMinuto(40.0)
                .horaSalidaEstimada(LocalTime.of(16, 0))
                .horaEncierroLimite(LocalTime.of(23, 0))
                .build();
        c.getRecorridos().add(tramo(TipoTramo.OFICIAL, 200));
        Escenario escenario = escenarioCon(c);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));

        assertThatThrownBy(() -> optimizacionService.optimizar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("hora oficial de paso");
    }

    @Test
    void lanza422SiFaltaElRecorridoOficial() {
        Cofradia c = cofradiaBase(1L, "Sin recorrido oficial")
                .velocidadMarchaMetrosMinuto(40.0)
                .horaSalidaEstimada(LocalTime.of(16, 0))
                .horaEncierroLimite(LocalTime.of(23, 0))
                .horaOficialPaso(LocalTime.of(18, 0))
                .build();
        c.getRecorridos().add(tramo(TipoTramo.IDA, 100));
        Escenario escenario = escenarioCon(c);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));

        assertThatThrownBy(() -> optimizacionService.optimizar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("recorrido de tipo OFICIAL");
    }

    @Test
    void acumulaErroresDeVariasCofradiasEnElMismoMensaje() {
        Cofradia sinVelocidad = cofradiaBase(1L, "Cofradia1")
                .horaSalidaEstimada(LocalTime.of(16, 0))
                .horaEncierroLimite(LocalTime.of(23, 0))
                .horaOficialPaso(LocalTime.of(18, 0))
                .build();
        sinVelocidad.getRecorridos().add(tramo(TipoTramo.OFICIAL, 200));

        Cofradia sinRecorrido = cofradiaBase(2L, "Cofradia2")
                .velocidadMarchaMetrosMinuto(40.0)
                .horaSalidaEstimada(LocalTime.of(16, 0))
                .horaEncierroLimite(LocalTime.of(23, 0))
                .horaOficialPaso(LocalTime.of(18, 0))
                .build();

        Escenario escenario = escenarioCon(sinVelocidad, sinRecorrido);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));

        assertThatThrownBy(() -> optimizacionService.optimizar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cofradia1")
                .hasMessageContaining("Cofradia2");
    }

    @Test
    void lanza422SiLaVentanaHorariaEsInviable() {
        Cofradia c = cofradiaBase(1L, "Ventana imposible")
                .velocidadMarchaMetrosMinuto(40.0)
                .horaSalidaEstimada(LocalTime.of(20, 0))
                .horaEncierroLimite(LocalTime.of(19, 0))
                .horaOficialPaso(LocalTime.of(18, 0))
                .build();
        c.getRecorridos().add(tramo(TipoTramo.OFICIAL, 200));
        Escenario escenario = escenarioCon(c);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));

        assertThatThrownBy(() -> optimizacionService.optimizar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ventana horaria es inviable");
    }

    // ---- resolucion correcta ----

    @Test
    void unaSolaCofradiaConTramoDeIdaAcumulaRetrasoSiLlegaTardeAlOficial() {
        Cofradia c = cofradiaBase(10L, "Solitaria")
                .velocidadMarchaMetrosMinuto(50.0)
                .horaSalidaEstimada(LocalTime.of(9, 0))
                .horaEncierroLimite(LocalTime.of(23, 0))
                .horaOficialPaso(LocalTime.of(9, 0))
                .build();
        c.getRecorridos().add(tramo(TipoTramo.IDA, 500));
        c.getRecorridos().add(tramo(TipoTramo.OFICIAL, 300));
        Escenario escenario = escenarioCon(c);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));
        permitirGuardado();

        ResultadoOptimizacion resultado = optimizacionService.optimizar(1L);

        assertThat(resultado.getRetrasoTotalMinutos()).isEqualTo(10);
        assertThat(resultado.getRetrasoMaximoMinutos()).isEqualTo(10);
        assertThat(resultado.getResultadosCofradias()).hasSize(1);
        assertThat(resultado.getResultadosCofradias().get(0).getOrdenAsignado()).isEqualTo(1);
        assertThat(resultado.getResultadosCofradias().get(0).getHoraEntradaCalculada()).isEqualTo(LocalTime.of(9, 10));
        assertThat(resultado.getResultadosCofradias().get(0).getHoraSalidaCalculada()).isEqualTo(LocalTime.of(9, 16));
        assertThat(resultado.getResultadosCofradias().get(0).getRetrasoMinutos()).isEqualTo(10);
    }

    @Test
    void sumaVariosTramosDelMismoTipoParaCalcularLaDistancia() {
        Cofradia c = cofradiaBase(10L, "Tramos partidos")
                .velocidadMarchaMetrosMinuto(40.0)
                .horaSalidaEstimada(LocalTime.of(9, 0))
                .horaEncierroLimite(LocalTime.of(23, 0))
                .horaOficialPaso(LocalTime.of(9, 0))
                .build();
        c.getRecorridos().add(tramo(TipoTramo.OFICIAL, 100));
        c.getRecorridos().add(tramo(TipoTramo.OFICIAL, 100));
        Escenario escenario = escenarioCon(c);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));
        permitirGuardado();

        ResultadoOptimizacion resultado = optimizacionService.optimizar(1L);

        // 200m totales / 40 m/min = 5 min de transito
        assertThat(resultado.getResultadosCofradias().get(0).getHoraSalidaCalculada()).isEqualTo(LocalTime.of(9, 5));
    }

    @Test
    void conMargenAmplioElRetrasoTotalEsCero() {
        Cofradia pollinica = cofradiaBase(1L, "Pollinica")
                .velocidadMarchaMetrosMinuto(40.0)
                .longitudCortejoMetros(450.5)
                .horaSalidaEstimada(LocalTime.of(16, 30))
                .horaEncierroLimite(LocalTime.of(22, 0))
                .horaOficialPaso(LocalTime.of(18, 15))
                .build();
        pollinica.getRecorridos().add(tramo(TipoTramo.OFICIAL, 210.5));

        Cofradia amor = cofradiaBase(2L, "Amor")
                .velocidadMarchaMetrosMinuto(35.0)
                .longitudCortejoMetros(300.0)
                .horaSalidaEstimada(LocalTime.of(16, 0))
                .horaEncierroLimite(LocalTime.of(23, 0))
                .horaOficialPaso(LocalTime.of(18, 10))
                .build();
        amor.getRecorridos().add(tramo(TipoTramo.OFICIAL, 210.5));

        Cofradia esperanza = cofradiaBase(3L, "Esperanza")
                .velocidadMarchaMetrosMinuto(30.0)
                .longitudCortejoMetros(600.0)
                .horaSalidaEstimada(LocalTime.of(17, 0))
                .horaEncierroLimite(LocalTime.of(23, 30))
                .horaOficialPaso(LocalTime.of(18, 12))
                .build();
        esperanza.getRecorridos().add(tramo(TipoTramo.OFICIAL, 210.5));

        Escenario escenario = escenarioCon(pollinica, amor, esperanza);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));
        permitirGuardado();

        ResultadoOptimizacion resultado = optimizacionService.optimizar(1L);

        assertThat(resultado.getRetrasoTotalMinutos()).isZero();
        assertThat(resultado.getRetrasoMaximoMinutos()).isZero();
        assertThat(resultado.getResultadosCofradias()).hasSize(3);
        assertThat(resultado.getResultadosCofradias())
                .extracting(rc -> rc.getOrdenAsignado())
                .containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void tresCofradiasIdenticasCompitiendoPorElMismoHuecoGeneranCascadaDeRetrasos() {
        List<Cofradia> cofradias = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Cofradia c = cofradiaBase((long) i, "Cofradia " + i)
                    .velocidadMarchaMetrosMinuto(40.0)
                    .longitudCortejoMetros(450.0)
                    .horaSalidaEstimada(LocalTime.of(18, 0))
                    .horaEncierroLimite(LocalTime.of(23, 0))
                    .horaOficialPaso(LocalTime.of(18, 5))
                    .build();
            c.getRecorridos().add(tramo(TipoTramo.OFICIAL, 210));
            cofradias.add(c);
        }
        Escenario escenario = escenarioCon(cofradias.toArray(new Cofradia[0]));
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));
        permitirGuardado();

        ResultadoOptimizacion resultado = optimizacionService.optimizar(1L);

        // duracion de ocupacion = ceil((210+450)/40) = 17 min por cofradia
        assertThat(resultado.getRetrasoTotalMinutos()).isEqualTo(0 + 12 + 29);
        assertThat(resultado.getRetrasoMaximoMinutos()).isEqualTo(29);
        assertThat(resultado.getResultadosCofradias()).hasSize(3);

        List<Integer> retrasosOrdenados = resultado.getResultadosCofradias().stream()
                .sorted((a, b) -> a.getOrdenAsignado().compareTo(b.getOrdenAsignado()))
                .map(rc -> rc.getRetrasoMinutos())
                .toList();
        assertThat(retrasosOrdenados).containsExactly(0, 12, 29);

        List<LocalTime> entradasOrdenadas = resultado.getResultadosCofradias().stream()
                .sorted((a, b) -> a.getOrdenAsignado().compareTo(b.getOrdenAsignado()))
                .map(rc -> rc.getHoraEntradaCalculada())
                .toList();
        assertThat(entradasOrdenadas).containsExactly(
                LocalTime.of(18, 0), LocalTime.of(18, 17), LocalTime.of(18, 34));
    }

    @Test
    void lanza422SiNoExisteHorarioFactible() {
        // Dos cofradias con ventana de entrada reducida a un unico instante identico:
        // salida 10:00, ocupacion = 400/40 = 10 min, encierro 10:10 -> unica entrada posible = 10:00 para ambas,
        // pero la segunda necesitaria entrar como pronto a las 10:10 -> infactible.
        Cofradia c1 = cofradiaBase(1L, "Rigida1")
                .velocidadMarchaMetrosMinuto(40.0)
                .horaSalidaEstimada(LocalTime.of(10, 0))
                .horaEncierroLimite(LocalTime.of(10, 10))
                .horaOficialPaso(LocalTime.of(10, 0))
                .build();
        c1.getRecorridos().add(tramo(TipoTramo.OFICIAL, 400));

        Cofradia c2 = cofradiaBase(2L, "Rigida2")
                .velocidadMarchaMetrosMinuto(40.0)
                .horaSalidaEstimada(LocalTime.of(10, 0))
                .horaEncierroLimite(LocalTime.of(10, 10))
                .horaOficialPaso(LocalTime.of(10, 0))
                .build();
        c2.getRecorridos().add(tramo(TipoTramo.OFICIAL, 400));

        Escenario escenario = escenarioCon(c1, c2);
        when(escenarioRepository.findById(1L)).thenReturn(Optional.of(escenario));

        assertThatThrownBy(() -> optimizacionService.optimizar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No existe un horario factible");
    }
}
