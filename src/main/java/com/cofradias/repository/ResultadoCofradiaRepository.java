package com.cofradias.repository;

import com.cofradias.model.ResultadoCofradia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultadoCofradiaRepository extends JpaRepository<ResultadoCofradia, Long> {

    List<ResultadoCofradia> findByResultadoOptimizacionId(Long resultadoOptimizacionId);
}
