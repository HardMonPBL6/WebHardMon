package com.telemetria.web.security;

import com.telemetria.web.model.Administrador;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

public class AdminPrincipal implements UserDetails {
    private final Administrador admin;

    public AdminPrincipal(Administrador admin) {
        this.admin = admin;
    }

    public Long getEmpresaId() {
        return admin.getEmpresa().getId();
    }
    
    public String getEmpresaNombre() {
        return admin.getEmpresa().getNombre();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getPassword() { return admin.getPassword(); }

    @Override
    public String getUsername() { return admin.getUsername(); }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
