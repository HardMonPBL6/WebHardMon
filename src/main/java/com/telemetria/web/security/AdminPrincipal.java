package com.telemetria.web.security;

import com.telemetria.web.model.Administrador;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AdminPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Long empresaId;
    private final String empresaNombre;

    public AdminPrincipal(Administrador admin) {
        this.id = admin.getId();
        this.username = admin.getUsername();
        this.password = admin.getPassword();

        if (admin.getEmpresa() == null) {
            throw new IllegalStateException("El administrador no tiene empresa asociada");
        }

        this.empresaId = admin.getEmpresa().getId();
        this.empresaNombre = admin.getEmpresa().getNombre();
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getEmpresaNombre() {
        return empresaNombre;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override 
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override 
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override 
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override 
    public boolean isEnabled() {
        return true;
    }
}