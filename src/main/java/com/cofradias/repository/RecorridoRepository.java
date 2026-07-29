package com.cofradias.repository;

import com.cofradias.model.Recorrido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecorridoRepository extends JpaRepository<Recorrido, Long> {

    List<Recorrido> findByCofradiaId(Long cofradiaId);
}
