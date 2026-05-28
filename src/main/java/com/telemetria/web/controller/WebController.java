package com.telemetria.web.controller;

import com.telemetria.web.model.Empresa;
import com.telemetria.web.model.Licencia;
import com.telemetria.web.repository.LicenciaRepository;
import com.telemetria.web.security.AdminPrincipal;
import com.telemetria.web.service.GrafanaService;
import com.telemetria.web.service.VictoriaMetricsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class WebController {

    private static final String LICENSE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GrafanaService grafanaService;
    private final VictoriaMetricsService victoriaMetricsService;
    private final LicenciaRepository licenciaRepository;

    public WebController(GrafanaService grafanaService,
                         VictoriaMetricsService victoriaMetricsService,
                         LicenciaRepository licenciaRepository) {
        this.grafanaService = grafanaService;
        this.victoriaMetricsService = victoriaMetricsService;
        this.licenciaRepository = licenciaRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/dashboard", "/dashboard/empresa"})
    public String dashboardEmpresa(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Long empresaId = principal.getEmpresaId();

        model.addAttribute("grafanaIframeUrl", grafanaService.buildEmpresaDashboardUrl(empresaId));
        model.addAttribute("empresaNombre", principal.getEmpresaNombre());
        model.addAttribute("empresaId", empresaId);
        model.addAttribute("dashboardTipo", "empresa");
        model.addAttribute("seccionActiva", "empresa");
        model.addAttribute("dashboardTitulo", "Enpresaren panela");
        model.addAttribute("portatiles", List.of());

        return "layout";
    }

    @GetMapping("/dashboard/ordenadores")
    public String dashboardOrdenadores(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Long empresaId = principal.getEmpresaId();

        model.addAttribute("grafanaIframeUrl", grafanaService.buildOrdenadoresDashboardUrl(empresaId));
        model.addAttribute("empresaNombre", principal.getEmpresaNombre());
        model.addAttribute("empresaId", empresaId);
        model.addAttribute("dashboardTipo", "ordenadores");
        model.addAttribute("seccionActiva", "ordenadores");
        model.addAttribute("dashboardTitulo", "Ordenagailuen panela");
        model.addAttribute("portatiles", victoriaMetricsService.findPortatilesByEmpresa(empresaId));

        return "layout";
    }

    @GetMapping("/licencias")
    public String licencias(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Long empresaId = principal.getEmpresaId();
        List<Licencia> licencias = licenciaRepository.findByEmpresaIdOrderByPortatilAsc(empresaId);
        List<String> portatilesVictoria = victoriaMetricsService.findPortatilesByEmpresa(empresaId);
        List<LicenciaOrdenadorView> licenciasOrdenadores = buildLicenciasOrdenadores(portatilesVictoria, licencias);
        List<String> portatilesSinLicencia = licenciasOrdenadores.stream()
                .filter(item -> item.licencia() == null)
                .map(LicenciaOrdenadorView::portatil)
                .toList();

        model.addAttribute("empresaNombre", principal.getEmpresaNombre());
        model.addAttribute("empresaId", empresaId);
        model.addAttribute("seccionActiva", "licencias");
        model.addAttribute("dashboardTitulo", "Lizentziak");
        model.addAttribute("licencias", licencias);
        model.addAttribute("licenciasOrdenadores", licenciasOrdenadores);
        model.addAttribute("portatilesSinLicencia", portatilesSinLicencia);
        model.addAttribute("portatilesDetectados", portatilesVictoria.size());
        model.addAttribute("codigoSugerido", generarCodigoLicenciaUnico());

        return "licencias";
    }

    @PostMapping("/licencias")
    public String crearLicencia(@AuthenticationPrincipal AdminPrincipal principal,
                                @RequestParam String portatil,
                                @RequestParam(required = false) String codigo,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        String portatilNormalizado = normalizar(portatil);
        String codigoNormalizado = normalizarCodigo(codigo);

        if (portatilNormalizado.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Ordenagailuaren izena adierazi behar duzu.");
            return "redirect:/licencias";
        }

        if (licenciaRepository.existsByEmpresaIdAndPortatil(principal.getEmpresaId(), portatilNormalizado)) {
            redirectAttributes.addFlashAttribute("error", "Ordenagailu horrek lizentzia bat esleituta du dagoeneko.");
            return "redirect:/licencias";
        }

        if (codigoNormalizado.isBlank()) {
            codigoNormalizado = generarCodigoLicenciaUnico();
        } else if (licenciaRepository.existsByCodigo(codigoNormalizado)) {
            redirectAttributes.addFlashAttribute("error", "Lizentzia-kode hori badago dagoeneko.");
            return "redirect:/licencias";
        }

        Empresa empresa = new Empresa();
        empresa.setId(principal.getEmpresaId());

        Licencia licencia = new Licencia();
        licencia.setEmpresa(empresa);
        licencia.setPortatil(portatilNormalizado);
        licencia.setCodigo(codigoNormalizado);
        licencia.setActiva(true);

        licenciaRepository.save(licencia);
        redirectAttributes.addFlashAttribute("success", "Lizentzia ondo sortu da.");
        return "redirect:/licencias";
    }

    @PostMapping("/licencias/{id}/borrar")
    public String borrarLicencia(@AuthenticationPrincipal AdminPrincipal principal,
                                 @PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        licenciaRepository.findByIdAndEmpresaId(id, principal.getEmpresaId())
                .ifPresentOrElse(
                        licenciaRepository::delete,
                        () -> redirectAttributes.addFlashAttribute("error", "Ez da lizentzia hori aurkitu zure enpresan.")
                );

        if (!redirectAttributes.getFlashAttributes().containsKey("error")) {
            redirectAttributes.addFlashAttribute("success", "Lizentzia ondo ezabatu da.");
        }

        return "redirect:/licencias";
    }

    private List<LicenciaOrdenadorView> buildLicenciasOrdenadores(List<String> portatilesVictoria, List<Licencia> licencias) {
        Map<String, Licencia> licenciaPorPortatil = licencias.stream()
                .collect(Collectors.toMap(Licencia::getPortatil, Function.identity(), (a, b) -> a));

        LinkedHashSet<String> portatiles = new LinkedHashSet<>();
        portatiles.addAll(portatilesVictoria);
        licencias.stream().map(Licencia::getPortatil).forEach(portatiles::add);

        List<LicenciaOrdenadorView> resultado = new ArrayList<>();
        for (String portatil : portatiles) {
            resultado.add(new LicenciaOrdenadorView(portatil, licenciaPorPortatil.get(portatil), portatilesVictoria.contains(portatil)));
        }
        return resultado;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String normalizarCodigo(String codigo) {
        return normalizar(codigo).replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private String generarCodigoLicenciaUnico() {
        String codigo;
        do {
            codigo = generarCodigoLicencia();
        } while (licenciaRepository.existsByCodigo(codigo));
        return codigo;
    }

    private String generarCodigoLicencia() {
        StringBuilder builder = new StringBuilder("WHM-");
        for (int bloque = 0; bloque < 4; bloque++) {
            if (bloque > 0) {
                builder.append('-');
            }
            for (int i = 0; i < 4; i++) {
                builder.append(LICENSE_ALPHABET.charAt(RANDOM.nextInt(LICENSE_ALPHABET.length())));
            }
        }
        return builder.toString();
    }

    public record LicenciaOrdenadorView(String portatil, Licencia licencia, boolean reportandoMetricas) {
    }
}
