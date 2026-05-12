package com.telemetria.web.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    
    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL)
    private List<Licencia> licencias;
    
    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL)
    private List<Administrador> administradores;
}
