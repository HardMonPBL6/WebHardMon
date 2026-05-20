package com.telemetria.web.controller;

import com.telemetria.web.security.AdminPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GrafanaAuthController {

    @Value("${app.internal.auth-token}")
    private String internalAuthToken;

    @GetMapping("/internal/auth/grafana")
    public ResponseEntity<Void> authGrafana(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            Authentication authentication,
            HttpServletResponse response
    ) {
        if (!internalAuthToken.equals(token)) {
            return ResponseEntity.status(403).build();
        }

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AdminPrincipal principal)) {
            return ResponseEntity.status(401).build();
        }

        response.setHeader("X-Grafana-User", principal.getUsername());
        response.setHeader("X-Grafana-Company", principal.getEmpresaId().toString());

        return ResponseEntity.ok().build();
    }
}