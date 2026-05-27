package com.telemetria.web.repository;

import com.telemetria.web.model.Licencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicenciaRepository extends JpaRepository<Licencia, Long> {

    List<Licencia> findByEmpresaIdOrderByPortatilAsc(Long empresaId);

    Optional<Licencia> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndPortatil(Long empresaId, String portatil);

    boolean existsByCodigo(String codigo);
}
