package com.telemetria.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GrafanaService {

    @Value("${app.grafana.base-url:/grafana}")
    private String grafanaBaseUrl;

    @Value("${app.grafana.dashboard-uid:ads2xlm}")
    private String dashboardUid;

    public String buildDashboardUrl(Long empresaId) {
        return UriComponentsBuilder
                .fromPath(grafanaBaseUrl + "/d/" + dashboardUid + "/dash")
                .queryParam("orgId", "1")
                .queryParam("theme", "dark")
                .queryParam("kiosk", "tv")
                .queryParam("var-empresa", empresaId)
                .build()
                .toUriString();
    }
}