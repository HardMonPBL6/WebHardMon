package com.telemetria.web.controller;

import com.telemetria.web.model.Administrador;
import com.telemetria.web.model.Empresa;
import com.telemetria.web.repository.AdministradorRepository;
import com.telemetria.web.repository.EmpresaRepository;
import com.telemetria.web.security.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT     = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL   = Pattern.compile("[^a-zA-Z0-9]");

    private final AdministradorRepository adminRepo;
    private final EmpresaRepository empresaRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminController(AdministradorRepository adminRepo,
                           EmpresaRepository empresaRepo,
                           PasswordEncoder passwordEncoder) {
        this.adminRepo = adminRepo;
        this.empresaRepo = empresaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/usuarios")
    public String listar(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("admins",       adminRepo.findAll());
        model.addAttribute("empresas",     empresaRepo.findAll());
        model.addAttribute("empresaNombre", principal.getEmpresaNombre());
        model.addAttribute("seccionActiva", "adminUsuarios");
        model.addAttribute("dashboardTitulo", "Administratzaileak");
        return "admins";
    }

    @PostMapping("/usuarios")
    public String crear(@AuthenticationPrincipal AdminPrincipal principal,
                        @RequestParam String username,
                        @RequestParam String password,
                        @RequestParam String confirmarPassword,
                        @RequestParam(required = false) Long empresaId,
                        @RequestParam(required = false) String empresaNueva,
                        RedirectAttributes ra) {

        String usernameN = username == null ? "" : username.trim();
        if (usernameN.isBlank()) {
            ra.addFlashAttribute("error", "Erabiltzaile-izena ezin da hutsik egon.");
            return "redirect:/admin/usuarios";
        }
        if (adminRepo.findByUsername(usernameN).isPresent()) {
            ra.addFlashAttribute("error", "Erabiltzaile-izen hori dagoeneko existitzen da.");
            return "redirect:/admin/usuarios";
        }

        String errorContrasena = validarContrasena(password, confirmarPassword);
        if (errorContrasena != null) {
            ra.addFlashAttribute("error", errorContrasena);
            return "redirect:/admin/usuarios";
        }

        Empresa empresa;
        String empresaNuevaN = empresaNueva == null ? "" : empresaNueva.trim();
        if (!empresaNuevaN.isBlank()) {
            Empresa nueva = new Empresa();
            nueva.setNombre(empresaNuevaN);
            empresa = empresaRepo.save(nueva);
        } else if (empresaId != null) {
            empresa = empresaRepo.findById(empresaId).orElse(null);
            if (empresa == null) {
                ra.addFlashAttribute("error", "Hautatutako enpresa ez da existitzen.");
                return "redirect:/admin/usuarios";
            }
        } else {
            ra.addFlashAttribute("error", "Hautatu enpresa bat edo eman izen berri bat.");
            return "redirect:/admin/usuarios";
        }

        Administrador admin = new Administrador();
        admin.setUsername(usernameN);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEmpresa(empresa);
        admin.setSuperAdmin(false);
        adminRepo.save(admin);

        ra.addFlashAttribute("success", "Administratzailea ondo sortu da: " + usernameN);
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/borrar")
    public String borrar(@AuthenticationPrincipal AdminPrincipal principal,
                         @PathVariable Long id,
                         RedirectAttributes ra) {

        if (id.equals(principal.getId())) {
            ra.addFlashAttribute("error", "Ezin duzu zeure kontu propioa ezabatu.");
            return "redirect:/admin/usuarios";
        }

        adminRepo.findById(id).ifPresentOrElse(
            admin -> {
                if (admin.isSuperAdmin()) {
                    ra.addFlashAttribute("error", "Ezin da superadmin bat ezabatu.");
                } else {
                    adminRepo.delete(admin);
                    ra.addFlashAttribute("success", "Administratzailea ezabatu da.");
                }
            },
            () -> ra.addFlashAttribute("error", "Ez da administratzaile hori aurkitu.")
        );

        return "redirect:/admin/usuarios";
    }

    private String validarContrasena(String password, String confirmar) {
        if (password == null || password.length() < 12) {
            return "Pasahitzak gutxienez 12 karaktere izan behar ditu.";
        }
        if (!UPPERCASE.matcher(password).find()) {
            return "Pasahitzak gutxienez maiuskula bat izan behar du.";
        }
        if (!DIGIT.matcher(password).find()) {
            return "Pasahitzak gutxienez zenbaki bat izan behar du.";
        }
        if (!SPECIAL.matcher(password).find()) {
            return "Pasahitzak gutxienez karaktere berezi bat izan behar du (!@#...).";
        }
        if (!password.equals(confirmar)) {
            return "Pasahitzak ez datoz bat.";
        }
        return null;
    }
}
