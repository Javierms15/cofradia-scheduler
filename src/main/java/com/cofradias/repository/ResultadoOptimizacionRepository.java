package com.cofradias.repository;

import com.cofradias.model.ResultadoOptimizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultadoOptimizacionRepository extends JpaRepository<ResultadoOptimizacion, Long> {

    List<ResultadoOptimizacion> findByEscenarioId(Long escenarioId);
}
