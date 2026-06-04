package com.telemetria.web.controller;

import com.telemetria.web.model.Empresa;
import com.telemetria.web.model.HourlyAggregateDTO;
import com.telemetria.web.model.Licencia;
import com.telemetria.web.model.Usuario;
import com.telemetria.web.repository.EmpresaRepository;
import com.telemetria.web.repository.LicenciaRepository;
import com.telemetria.web.repository.UsuarioRepository;
import com.telemetria.web.security.AdminPrincipal;
import com.telemetria.web.service.CassandraTelemetryService;
import com.telemetria.web.service.GrafanaService;
import com.telemetria.web.service.HBaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.dao.DataAccessException;

import java.security.SecureRandom;
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
    private final CassandraTelemetryService cassandraTelemetryService;
    private final HBaseService hBaseService;
    private final LicenciaRepository licenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public WebController(GrafanaService grafanaService,
                         CassandraTelemetryService cassandraTelemetryService,
                         HBaseService hBaseService,
                         LicenciaRepository licenciaRepository,
                         UsuarioRepository usuarioRepository,
                         EmpresaRepository empresaRepository) {
        this.grafanaService = grafanaService;
        this.cassandraTelemetryService = cassandraTelemetryService;
        this.hBaseService = hBaseService;
        this.licenciaRepository = licenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    // ── Dashboards ───────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/dashboard", "/dashboard/empresa"})
    public String dashboardEmpresa(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (principal == null) return "redirect:/login";
        Long empresaId = principal.getEmpresaId();

        model.addAttribute("grafanaIframeUrl", grafanaService.buildEmpresaDashboardUrl(empresaId));
        model.addAttribute("empresaNombre",   principal.getEmpresaNombre());
        model.addAttribute("empresaId",       empresaId);
        model.addAttribute("dashboardTipo",   "empresa");
        model.addAttribute("seccionActiva",   "empresa");
        model.addAttribute("dashboardTitulo", "Enpresaren panela");
        model.addAttribute("portatiles",      List.of());
        model.addAttribute("isSuperAdmin",    principal.isSuperAdmin());
        return "layout";
    }

    @GetMapping("/dashboard/ordenadores")
    public String dashboardOrdenadores(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (principal == null) return "redirect:/login";
        Long empresaId = principal.getEmpresaId();

        model.addAttribute("grafanaIframeUrl", grafanaService.buildOrdenadoresDashboardUrl(empresaId));
        model.addAttribute("empresaNombre",   principal.getEmpresaNombre());
        model.addAttribute("empresaId",       empresaId);
        model.addAttribute("dashboardTipo",   "ordenadores");
        model.addAttribute("seccionActiva",   "ordenadores");
        model.addAttribute("dashboardTitulo", "Ordenagailuen panela");
        model.addAttribute("portatiles",      cassandraTelemetryService.findNombresByEmpresa(empresaId));
        model.addAttribute("isSuperAdmin",    principal.isSuperAdmin());
        return "layout";
    }

    // ── Histórico HBase ──────────────────────────────────────────────────────

    @GetMapping("/api/historico")
    @ResponseBody
    public ResponseEntity<List<HourlyAggregateDTO>> historico(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(defaultValue = "7") int days) {
        if (principal == null) return ResponseEntity.status(401).build();
        List<HourlyAggregateDTO> data = hBaseService.getRecentAggregates(
                principal.getEmpresaId(), days);
        return ResponseEntity.ok(data);
    }

    // ── Licencias y usuarios ─────────────────────────────────────────────────

    @GetMapping("/licencias")
    public String licencias(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        if (principal == null) return "redirect:/login";
        Long empresaId = principal.getEmpresaId();

        List<Usuario>  usuarios  = usuarioRepository.findByEmpresaIdOrderByNombreAsc(empresaId);
        List<Licencia> licencias = licenciaRepository.findByUsuarioEmpresaIdOrderByUsuarioNombreAsc(empresaId);
        List<String>   enCassandra = cassandraTelemetryService.findNombresByEmpresa(empresaId);

        // Mapa usuarioId -> licencia para construir las vistas sin N+1
        Map<Long, Licencia> licenciaPorUsuario = licencias.stream()
                .collect(Collectors.toMap(
                        l -> l.getUsuario().getId(),
                        Function.identity(),
                        (a, b) -> a));

        List<UsuarioLicenciaView> vistas = usuarios.stream()
                .map(u -> new UsuarioLicenciaView(
                        u,
                        licenciaPorUsuario.get(u.getId()),
                        enCassandra.contains(u.getNombreOrdenador())))
                .collect(Collectors.toList());

        List<Usuario> usuariosSinLicencia = vistas.stream()
                .filter(v -> v.licencia() == null)
                .map(UsuarioLicenciaView::usuario)
                .toList();

        model.addAttribute("vistas",               vistas);
        model.addAttribute("licencias",            licencias);
        model.addAttribute("usuariosSinLicencia",  usuariosSinLicencia);
        model.addAttribute("portatilesDetectados", enCassandra.size());
        model.addAttribute("codigoSugerido",       generarCodigoLicenciaUnico());
        model.addAttribute("empresaNombre",        principal.getEmpresaNombre());
        model.addAttribute("empresaId",            empresaId);
        model.addAttribute("seccionActiva",        "licencias");
        model.addAttribute("dashboardTitulo",      "Lizentziak");
        model.addAttribute("isSuperAdmin",         principal.isSuperAdmin());
        return "licencias";
    }

    /** Crear un nuevo usuario (empleado + ordenador) dentro de la empresa. */
    @PostMapping("/usuarios")
    public String crearUsuario(@AuthenticationPrincipal AdminPrincipal principal,
                               @RequestParam String nombre,
                               @RequestParam String nombreOrdenador,
                               RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";

        String nombreN    = normalizar(nombre);
        String ordenadorN = normalizar(nombreOrdenador);

        if (nombreN.isBlank() || ordenadorN.isBlank()) {
            ra.addFlashAttribute("error", "Izen eta ordenagailu izena derrigorrezkoak dira.");
            return "redirect:/licencias";
        }
        if (usuarioRepository.existsByEmpresaIdAndNombreOrdenador(principal.getEmpresaId(), ordenadorN)) {
            ra.addFlashAttribute("error", "Ordenagailu izen hori dagoeneko erregistratuta dago.");
            return "redirect:/licencias";
        }

        Empresa empresa = empresaRepository.getReferenceById(principal.getEmpresaId());

        Usuario usuario = new Usuario();
        usuario.setNombre(nombreN);
        usuario.setNombreOrdenador(ordenadorN);
        usuario.setEmpresa(empresa);
        usuarioRepository.save(usuario);

        ra.addFlashAttribute("success", "Erabiltzailea ondo sortu da.");
        return "redirect:/licencias";
    }

    /** Borrar un usuario y su licencia (si tiene). */
    @PostMapping("/usuarios/{id}/borrar")
    public String borrarUsuario(@AuthenticationPrincipal AdminPrincipal principal,
                                @PathVariable Long id,
                                RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";

        usuarioRepository.findByIdAndEmpresaId(id, principal.getEmpresaId())
                .ifPresentOrElse(
                        usuarioRepository::delete,
                        () -> ra.addFlashAttribute("error", "Ez da erabiltzaile hori aurkitu zure enpresan."));

        if (!ra.getFlashAttributes().containsKey("error")) {
            ra.addFlashAttribute("success", "Erabiltzailea eta bere lizentzia ezabatu dira.");
        }
        return "redirect:/licencias";
    }

    /** Asignar una licencia a un usuario existente. */
    @PostMapping("/licencias")
    public String crearLicencia(@AuthenticationPrincipal AdminPrincipal principal,
                                @RequestParam(required = false) String usuarioId,
                                @RequestParam(required = false) String codigo,
                                RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        if (usuarioId == null || usuarioId.isBlank()) {
            ra.addFlashAttribute("error", "Hautatu erabiltzaile bat.");
            return "redirect:/licencias";
        }
        Long usuarioIdLong;
        try {
            usuarioIdLong = Long.parseLong(usuarioId.trim());
        } catch (NumberFormatException e) {
            ra.addFlashAttribute("error", "Erabiltzaile baliogabea.");
            return "redirect:/licencias";
        }

        String codigoN = normalizarCodigo(codigo);

        // Verificar que el usuario pertenece a esta empresa
        Usuario usuario = usuarioRepository
                .findByIdAndEmpresaId(usuarioIdLong, principal.getEmpresaId())
                .orElse(null);
        if (usuario == null) {
            ra.addFlashAttribute("error", "Ez da erabiltzaile hori aurkitu zure enpresan.");
            return "redirect:/licencias";
        }
        if (licenciaRepository.existsByUsuarioId(usuarioIdLong)) {
            ra.addFlashAttribute("error", "Erabiltzaile honek lizentzia bat esleituta du dagoeneko.");
            return "redirect:/licencias";
        }
        if (codigoN.isBlank()) {
            codigoN = generarCodigoLicenciaUnico();
        } else if (licenciaRepository.existsByCodigo(codigoN)) {
            ra.addFlashAttribute("error", "Lizentzia-kode hori badago dagoeneko.");
            return "redirect:/licencias";
        }

        Licencia licencia = new Licencia();
        // getReferenceById devuelve un proxy gestionado, evita el problema de
        // entidad "detached" al persistir en una nueva transacción de Hibernate 6.
        licencia.setUsuario(usuarioRepository.getReferenceById(usuarioIdLong));
        licencia.setCodigo(codigoN);
        licencia.setActiva(true);
        try {
            licenciaRepository.save(licencia);
        } catch (DataAccessException e) {
            ra.addFlashAttribute("error", "Lizentzia sortzean gatazka bat gertatu da (kodea edo erabiltzailea dagoeneko existitzen da).");
            return "redirect:/licencias";
        }

        ra.addFlashAttribute("success", "Lizentzia ondo sortu da.");
        return "redirect:/licencias";
    }

    /** Borrar una licencia (el usuario queda sin licencia pero no se elimina). */
    @PostMapping("/licencias/{id}/borrar")
    public String borrarLicencia(@AuthenticationPrincipal AdminPrincipal principal,
                                 @PathVariable Long id,
                                 RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";

        licenciaRepository.findByIdAndUsuarioEmpresaId(id, principal.getEmpresaId())
                .ifPresentOrElse(
                        licenciaRepository::delete,
                        () -> ra.addFlashAttribute("error", "Ez da lizentzia hori aurkitu zure enpresan."));

        if (!ra.getFlashAttributes().containsKey("error")) {
            ra.addFlashAttribute("success", "Lizentzia ondo ezabatu da.");
        }
        return "redirect:/licencias";
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String normalizarCodigo(String codigo) {
        return normalizar(codigo).replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private String generarCodigoLicenciaUnico() {
        String codigo;
        do { codigo = generarCodigoLicencia(); }
        while (licenciaRepository.existsByCodigo(codigo));
        return codigo;
    }

    private String generarCodigoLicencia() {
        StringBuilder sb = new StringBuilder("WHM-");
        for (int b = 0; b < 4; b++) {
            if (b > 0) sb.append('-');
            for (int i = 0; i < 4; i++)
                sb.append(LICENSE_ALPHABET.charAt(RANDOM.nextInt(LICENSE_ALPHABET.length())));
        }
        return sb.toString();
    }

    // ── Vista ────────────────────────────────────────────────────────────────

    /**
     * Vista que agrupa un usuario con su licencia (si tiene) y si esta
     * reportando metricas en Cassandra.
     */
    public record UsuarioLicenciaView(
            Usuario usuario,
            Licencia licencia,
            boolean reportandoMetricas) {
    }
}
