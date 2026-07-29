package com.cofradias.repository;

import com.cofradias.model.Cofradia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CofradiaRepository extends JpaRepository<Cofradia, Long> {

    List<Cofradia> findByEscenarioId(Long escenarioId);
}
