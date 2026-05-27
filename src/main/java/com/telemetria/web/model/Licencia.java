package com.telemetria.web.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
    name = "licencia",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_licencia_codigo", columnNames = "codigo"),
        @UniqueConstraint(name = "uk_licencia_empresa_portatil", columnNames = {"empresa_id", "portatil"})
    }
)
public class Licencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String portatil;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
