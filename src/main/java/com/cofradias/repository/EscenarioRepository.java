package com.cofradias.repository;

import com.cofradias.model.Escenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscenarioRepository extends JpaRepository<Escenario, Long> {

    List<Escenario> findByUsuarioId(Long usuarioId);
}
