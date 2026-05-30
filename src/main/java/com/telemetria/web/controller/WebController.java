package com.telemetria.web.controller;

import com.telemetria.web.security.AdminPrincipal;
import com.telemetria.web.service.GrafanaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    private final GrafanaService grafanaService;

    public WebController(GrafanaService grafanaService) {
        this.grafanaService = grafanaService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        String grafanaUrl = grafanaService.buildPublicDashboardUrl();
        if (grafanaUrl == null) {
            // En un proyecto real leerías el tenant del usuario (ej: Dashboard ID general)
            // Para este ejemplo usamos el UID real de tu dashboard Grafana
            grafanaUrl = grafanaService.buildDashboardUrl("ads2xlm", principal.getEmpresaId());
        }

        model.addAttribute("grafanaIframeUrl", grafanaUrl);
        model.addAttribute("empresaNombre", principal.getEmpresaNombre());
        
        return "layout";
    }

    @GetMapping({"/", "/licencias"})
    public String root() {
        return "redirect:/dashboard";
    }
}