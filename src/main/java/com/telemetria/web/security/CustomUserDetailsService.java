package com.telemetria.web.security;

import com.telemetria.web.model.Administrador;
import com.telemetria.web.repository.AdministradorRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdministradorRepository administradorRepository;

    public CustomUserDetailsService(AdministradorRepository administradorRepository) {
        this.administradorRepository = administradorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Administrador admin = administradorRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Erabiltzailea ez da aurkitu: " + username));
        return new AdminPrincipal(admin);
    }
}