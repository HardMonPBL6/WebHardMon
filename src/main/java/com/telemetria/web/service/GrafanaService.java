package com.telemetria.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GrafanaService {

    @Value("${app.grafana.dashboard-url}")
    private String dashboardUrl;

    public String buildDashboardUrl(Long empresaId) {
        if (empresaId == null) {
            throw new IllegalArgumentException("El usuario autenticado no tiene empresa asociada");
        }

        return UriComponentsBuilder
                .fromUriString(dashboardUrl)
                .queryParam("orgId", "1")
                .queryParam("theme", "dark")
                .queryParam("kiosk", "tv")
                .queryParam("var-empresa", empresaId)
                .build()
                .toUriString();
    }
}