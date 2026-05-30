package com.telemetria.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GrafanaService {

    @Value("${app.grafana.base-url}")
    private String grafanaBaseUrl;

    @Value("${app.grafana.public-url:}")
    private String grafanaPublicUrl;

    /**
     * Devuelve la URL del iFrame de Grafana incluyendo el parámetro de tenant.
     * En Grafana se debe configurar la variable de dashboard "var-empresa" para filtrar métricas.
     */
    public String buildDashboardUrl(String dashboardUid, Long empresaId) {
        // Grafana kiosk mode (theme=dark y panel limpio sin menú lateral)
        return String.format("%s/d/%s/dash?orgId=1&theme=dark&kiosk=tv&var-empresa=%d", 
                grafanaBaseUrl, dashboardUid, empresaId);
    }

    /**
     * Devuelve la URL pública completa del dashboard, si está configurada.
     */
    public String buildPublicDashboardUrl() {
        return (grafanaPublicUrl != null && !grafanaPublicUrl.isBlank())
                ? grafanaPublicUrl
                : null;
    }
}
