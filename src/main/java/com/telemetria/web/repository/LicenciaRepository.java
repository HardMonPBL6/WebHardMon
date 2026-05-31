package com.telemetria.web.repository;

import com.telemetria.web.model.Licencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicenciaRepository extends JpaRepository<Licencia, Long> {

    @EntityGraph(attributePaths = {"usuario", "usuario.empresa"})
    @Query("SELECT l FROM Licencia l JOIN l.usuario u WHERE l.codigo = :codigo")
    Optional<Licencia> findByCodigo(@Param("codigo") String codigo);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Licencia l WHERE l.codigo = :codigo")
    boolean existsByCodigo(@Param("codigo") String codigo);

    @EntityGraph(attributePaths = "usuario")
    @Query("SELECT l FROM Licencia l JOIN l.usuario u WHERE u.id = :usuarioId")
    Optional<Licencia> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Licencia l WHERE l.usuario.id = :usuarioId")
    boolean existsByUsuarioId(@Param("usuarioId") Long usuarioId);

    @EntityGraph(attributePaths = "usuario")
    @Query("SELECT l FROM Licencia l JOIN l.usuario u WHERE u.empresa.id = :empresaId ORDER BY u.nombre ASC")
    List<Licencia> findByUsuarioEmpresaIdOrderByUsuarioNombreAsc(@Param("empresaId") Long empresaId);

    @EntityGraph(attributePaths = "usuario")
    @Query("SELECT l FROM Licencia l JOIN l.usuario u WHERE l.id = :id AND u.empresa.id = :empresaId")
    Optional<Licencia> findByIdAndUsuarioEmpresaId(@Param("id") Long id, @Param("empresaId") Long empresaId);
}
