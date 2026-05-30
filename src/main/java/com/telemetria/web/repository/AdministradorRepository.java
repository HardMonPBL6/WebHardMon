package com.telemetria.web.repository;

import com.telemetria.web.model.Administrador;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
    @EntityGraph(attributePaths = "empresa")
    Optional<Administrador> findByUsername(String username);
}