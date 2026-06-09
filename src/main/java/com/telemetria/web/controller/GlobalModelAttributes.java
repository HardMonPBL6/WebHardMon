package com.telemetria.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expone atributos comunes a TODAS las vistas Thymeleaf.
 *
 * <p>matomoUrl: URL pública de Matomo (con barra final) que el snippet de tracking
 * usa para cargar matomo.js/matomo.php. Lo descarga el navegador del usuario final,
 * así que debe ser una URL pública (el Cloudflare Tunnel matomo.&lt;zona&gt;), no
 * localhost. Si {@code app.matomo.url} está vacío, el atributo es null y las
 * plantillas omiten el snippet (analítica desactivada).
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final String matomoUrl;

    public GlobalModelAttributes(@Value("${app.matomo.url:}") String matomoUrl) {
        this.matomoUrl = (matomoUrl == null || matomoUrl.isBlank()) ? null : matomoUrl;
    }

    @ModelAttribute("matomoUrl")
    public String matomoUrl() {
        return matomoUrl;
    }
}
