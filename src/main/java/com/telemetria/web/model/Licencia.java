package com.telemetria.web.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Licencia de ejecucion del agente Go.
 * Una licencia pertenece a un unico usuario (1-1) e identifica tanto
 * al empleado como a su ordenador (usuario.nombreOrdenador).
 * El codigo es el secreto que el agente Go envia al arrancar para
 * validarse contra la API web.
 */
@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
    name = "licencia",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_licencia_codigo",  columnNames = "codigo"),
        @UniqueConstraint(name = "uk_licencia_usuario", columnNames = "usuario_id")
    }
)
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 255)
    private String codigo;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Usuario al que pertenece esta licencia.
     * A traves de usuario se obtiene: empresa_id, nombre_ordenador.
     */
    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
