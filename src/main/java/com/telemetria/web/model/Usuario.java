package com.telemetria.web.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Empleado de la empresa que tiene un ordenador y puede tener una licencia
 * para ejecutar el agente Go. NO tiene acceso al panel web.
 */
@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
    name = "usuario",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_usuario_empresa_ordenador",
            columnNames = {"empresa_id", "nombre_ordenador"}
        )
    }
)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Nombre real del empleado (solo informativo). */
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Nombre del ordenador: clave que se usa en Cassandra como campo 'nombre'
     * en las tablas ordenadores y mediciones.
     */
    @Column(name = "nombre_ordenador", nullable = false, length = 80)
    private String nombreOrdenador;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /**
     * Licencia asignada a este usuario. Puede ser null si todavia no
     * se le ha creado una. Al borrar el usuario se borra tambien su licencia.
     */
    @ToString.Exclude
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY,
              cascade = CascadeType.ALL, orphanRemoval = true)
    private Licencia licencia;
}
