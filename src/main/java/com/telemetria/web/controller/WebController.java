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
        if (principal == null) {
            return "redirect:/login";
        }

        Long empresaId = principal.getEmpresaId();

        String grafanaUrl = grafanaService.buildDashboardUrl(empresaId);

        model.addAttribute("grafanaIframeUrl", grafanaUrl);
        model.addAttribute("empresaNombre", principal.getEmpresaNombre());
        model.addAttribute("empresaId", empresaId);

        return "layout";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/licencias")
    public String licencias() {
        return "redirect:/dashboard";
    }
}