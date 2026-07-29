package com.cofradias.optimization;

import com.cofradias.model.Cofradia;
import com.cofradias.model.Escenario;
import com.cofradias.model.Recorrido;
import com.cofradias.model.ResultadoCofradia;
import com.cofradias.model.ResultadoOptimizacion;
import com.cofradias.model.TipoTramo;
import com.cofradias.repository.EscenarioRepository;
import com.cofradias.repository.ResultadoOptimizacionRepository;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Modela el recorrido oficial como un unico recurso sin adelantamiento (como una via de tren):
 * cada cofradia ocupa el tramo oficial desde que entra la cabeza hasta que sale el final del
 * cortejo, y dos cofradias no pueden solaparse en esa ocupacion. Minimiza el retraso total
 * respecto a la hora oficial de paso publicada.
 *
 * Simplificacion actual: no se admite que una cofradia cruce la medianoche (hora_encierro_limite
 * debe ser posterior, en reloj, a hora_salida_estimada).
 */
@Service
public class OptimizacionService {

    private static final int MINUTOS_POR_DIA = 24 * 60;

    private final EscenarioRepository escenarioRepository;
    private final ResultadoOptimizacionRepository resultadoOptimizacionRepository;

    public OptimizacionService(EscenarioRepository escenarioRepository,
                                ResultadoOptimizacionRepository resultadoOptimizacionRepository) {
        this.escenarioRepository = escenarioRepository;
        this.resultadoOptimizacionRepository = resultadoOptimizacionRepository;
    }

    @Transactional
    public ResultadoOptimizacion optimizar(Long escenarioId) {
        Escenario escenario = escenarioRepository.findById(escenarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escenario no encontrado"));

        List<Cofradia> cofradias = escenario.getCofradias();
        if (cofradias.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El escenario no tiene cofradias");
        }

        List<String> errores = new ArrayList<>();
        List<DatosCofradia> datos = new ArrayList<>();
        for (Cofradia cofradia : cofradias) {
            DatosCofradia d = calcularDatos(cofradia, errores);
            if (d != null) {
                datos.add(d);
            }
        }
        if (!errores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, String.join("; ", errores));
        }

        Resolucion resolucion = resolver(datos);
        return persistirResultado(escenario, datos, resolucion);
    }

    private DatosCofradia calcularDatos(Cofradia cofradia, List<String> errores) {
        String nombre = cofradia.getNombre();

        if (cofradia.getVelocidadMarchaMetrosMinuto() == null || cofradia.getVelocidadMarchaMetrosMinuto() <= 0) {
            errores.add(nombre + ": falta la velocidad de marcha");
        }
        if (cofradia.getHoraSalidaEstimada() == null) {
            errores.add(nombre + ": falta la hora de salida estimada");
        }
        if (cofradia.getHoraEncierroLimite() == null) {
            errores.add(nombre + ": falta la hora de encierro limite");
        }
        if (cofradia.getHoraOficialPaso() == null) {
            errores.add(nombre + ": falta la hora oficial de paso");
        }
        double distanciaOficial = sumarDistancia(cofradia.getRecorridos(), TipoTramo.OFICIAL);
        if (distanciaOficial <= 0) {
            errores.add(nombre + ": falta el recorrido de tipo OFICIAL");
        }

        boolean datosBasicosCompletos = cofradia.getVelocidadMarchaMetrosMinuto() != null
                && cofradia.getVelocidadMarchaMetrosMinuto() > 0
                && cofradia.getHoraSalidaEstimada() != null
                && cofradia.getHoraEncierroLimite() != null
                && cofradia.getHoraOficialPaso() != null
                && distanciaOficial > 0;
        if (!datosBasicosCompletos) {
            return null;
        }

        double velocidad = cofradia.getVelocidadMarchaMetrosMinuto();
        double longitudCortejo = cofradia.getLongitudCortejoMetros() != null ? cofradia.getLongitudCortejoMetros() : 0.0;
        double distanciaIda = sumarDistancia(cofradia.getRecorridos(), TipoTramo.IDA);
        double distanciaVuelta = sumarDistancia(cofradia.getRecorridos(), TipoTramo.VUELTA);

        int duracionIda = (int) Math.ceil(distanciaIda / velocidad);
        int duracionOficial = (int) Math.ceil(distanciaOficial / velocidad);
        int duracionVuelta = (int) Math.ceil(distanciaVuelta / velocidad);
        int duracionOcupacion = (int) Math.ceil((distanciaOficial + longitudCortejo) / velocidad);

        int salidaMin = cofradia.getHoraSalidaEstimada().toSecondOfDay() / 60;
        int encierroMin = cofradia.getHoraEncierroLimite().toSecondOfDay() / 60;
        int horaOficialPasoMin = cofradia.getHoraOficialPaso().toSecondOfDay() / 60;

        int entradaMinima = salidaMin + duracionIda;
        int entradaMaxima = encierroMin - duracionOcupacion - duracionVuelta;

        if (entradaMinima > entradaMaxima) {
            errores.add(nombre + ": la ventana horaria es inviable con los datos actuales "
                    + "(revisa hora de salida, hora de encierro y velocidad; no se admite cruzar la medianoche)");
            return null;
        }

        return new DatosCofradia(cofradia, entradaMinima, entradaMaxima, duracionOficial, duracionOcupacion, horaOficialPasoMin);
    }

    private double sumarDistancia(List<Recorrido> recorridos, TipoTramo tipo) {
        return recorridos.stream()
                .filter(r -> r.getTipo() == tipo)
                .mapToDouble(r -> r.getDistanciaMetros() != null ? r.getDistanciaMetros() : 0.0)
                .sum();
    }

    private record Resolucion(Solution solucion, IntVar[] entrada, IntVar[] retraso, IntVar retrasoTotal) {
    }

    private Resolucion resolver(List<DatosCofradia> datos) {
        Model model = new Model("optimizacion");
        int n = datos.size();
        IntVar[] entrada = new IntVar[n];
        IntVar[] retraso = new IntVar[n];

        for (int i = 0; i < n; i++) {
            DatosCofradia d = datos.get(i);
            entrada[i] = model.intVar("entrada_" + d.cofradia().getId(), d.entradaMinima(), d.entradaMaxima());
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                DatosCofradia di = datos.get(i);
                DatosCofradia dj = datos.get(j);
                model.or(
                        model.arithm(entrada[j], ">=", entrada[i], "+", di.duracionOcupacion()),
                        model.arithm(entrada[i], ">=", entrada[j], "+", dj.duracionOcupacion())
                ).post();
            }
        }

        IntVar cero = model.intVar(0);
        for (int i = 0; i < n; i++) {
            DatosCofradia d = datos.get(i);
            IntVar diferencia = model.intVar("diferencia_" + d.cofradia().getId(), -MINUTOS_POR_DIA, MINUTOS_POR_DIA);
            model.arithm(diferencia, "=", entrada[i], "-", d.horaOficialPasoMin()).post();
            retraso[i] = model.intVar("retraso_" + d.cofradia().getId(), 0, MINUTOS_POR_DIA);
            model.max(retraso[i], diferencia, cero).post();
        }

        IntVar retrasoTotal = model.intVar("retrasoTotal", 0, MINUTOS_POR_DIA * n);
        model.sum(retraso, "=", retrasoTotal).post();

        Solver solver = model.getSolver();
        solver.limitTime("30s");
        Solution solucion = solver.findOptimalSolution(retrasoTotal, false);

        if (solucion == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No existe un horario factible: las ventanas de tiempo de las cofradias son incompatibles "
                            + "entre si en un unico recorrido oficial sin adelantamientos");
        }

        return new Resolucion(solucion, entrada, retraso, retrasoTotal);
    }

    private ResultadoOptimizacion persistirResultado(Escenario escenario, List<DatosCofradia> datos, Resolucion resolucion) {
        record Fila(Cofradia cofradia, int entradaMin, int salidaMin, int retrasoMin) {
        }

        Solution solucion = resolucion.solucion();
        List<Fila> filas = new ArrayList<>();
        for (int i = 0; i < datos.size(); i++) {
            DatosCofradia d = datos.get(i);
            int entradaVal = solucion.getIntVal(resolucion.entrada()[i]);
            int salidaVal = entradaVal + d.duracionOficial();
            int retrasoVal = solucion.getIntVal(resolucion.retraso()[i]);
            filas.add(new Fila(d.cofradia(), entradaVal, salidaVal, retrasoVal));
        }
        filas.sort(Comparator.comparingInt(Fila::entradaMin));

        ResultadoOptimizacion resultado = ResultadoOptimizacion.builder()
                .escenario(escenario)
                .retrasoTotalMinutos(solucion.getIntVal(resolucion.retrasoTotal()))
                .retrasoMaximoMinutos(filas.stream().mapToInt(Fila::retrasoMin).max().orElse(0))
                .build();

        for (int i = 0; i < filas.size(); i++) {
            Fila fila = filas.get(i);
            ResultadoCofradia rc = ResultadoCofradia.builder()
                    .resultadoOptimizacion(resultado)
                    .cofradia(fila.cofradia())
                    .ordenAsignado(i + 1)
                    .horaEntradaCalculada(minutosALocalTime(fila.entradaMin()))
                    .horaSalidaCalculada(minutosALocalTime(fila.salidaMin()))
                    .retrasoMinutos(fila.retrasoMin())
                    .build();
            resultado.getResultadosCofradias().add(rc);
        }

        return resultadoOptimizacionRepository.save(resultado);
    }

    private LocalTime minutosALocalTime(int minutos) {
        int m = ((minutos % MINUTOS_POR_DIA) + MINUTOS_POR_DIA) % MINUTOS_POR_DIA;
        return LocalTime.of(m / 60, m % 60);
    }

    private record DatosCofradia(
            Cofradia cofradia,
            int entradaMinima,
            int entradaMaxima,
            int duracionOficial,
            int duracionOcupacion,
            int horaOficialPasoMin
    ) {
    }
}
