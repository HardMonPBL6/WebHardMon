package com.telemetria.web.repository;

import com.telemetria.web.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u WHERE u.empresa.id = :empresaId ORDER BY u.nombre ASC")
    List<Usuario> findByEmpresaIdOrderByNombreAsc(@Param("empresaId") Long empresaId);

    @Query("SELECT u FROM Usuario u WHERE u.id = :id AND u.empresa.id = :empresaId")
    Optional<Usuario> findByIdAndEmpresaId(@Param("id") Long id, @Param("empresaId") Long empresaId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Usuario u WHERE u.empresa.id = :empresaId AND u.nombreOrdenador = :nombreOrdenador")
    boolean existsByEmpresaIdAndNombreOrdenador(@Param("empresaId") Long empresaId,
                                                @Param("nombreOrdenador") String nombreOrdenador);
}
