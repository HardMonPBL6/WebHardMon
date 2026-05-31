package com.telemetria.web.controller;

import com.telemetria.web.model.Licencia;
import com.telemetria.web.model.Usuario;
import com.telemetria.web.repository.LicenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Optional;

/**
 * Endpoint que consume el agente Go para validar su licencia antes de
 * empezar a enviar telemetria a Cassandra.
 *
 * El agente solo necesita su codigo de licencia: si es valido y esta activo,
 * este endpoint devuelve el empresaId y el nombreOrdenador que necesita para
 * escribir en Cassandra (empresa_id es la partition key y nombre identifica
 * al ordenador en las tablas ordenadores y mediciones).
 */
@RestController
@RequestMapping("/api/agente")
public class AgenteController {

    private static final Logger log = LoggerFactory.getLogger(AgenteController.class);

    private final LicenciaRepository licenciaRepository;

    public AgenteController(LicenciaRepository licenciaRepository) {
        this.licenciaRepository = licenciaRepository;
    }

    /**
     * Valida una licencia.
     *
     * Peticion (JSON):
     *   { "codigo": "WHM-XXXX-XXXX-XXXX-XXXX", "nombreOrdenador": "PC-1-1" }
     *   - codigo:          obligatorio.
     *   - nombreOrdenador: opcional. Si se envia, debe coincidir con el
     *                      ordenador asignado en la licencia.
     *
     * Respuestas:
     *   200 -> { "valida": true,  "empresaId": 1, "empresaNombre": "Acme", "nombreOrdenador": "PC-1-1" }
     *   400 -> codigo vacio
     *   403 -> licencia desactivada o nombreOrdenador no coincide
     *   404 -> codigo no existe
     */
    @PostMapping("/validar")
    @Transactional(readOnly = true)
    public ResponseEntity<ValidacionResponse> validar(
            @RequestBody(required = false) ValidacionRequest peticion) {

        String codigo = normalizarCodigo(peticion == null ? null : peticion.codigo());

        if (codigo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ValidacionResponse.invalida("Lizentzia-kodea derrigorrezkoa da."));
        }

        Optional<Licencia> encontrada = licenciaRepository.findByCodigo(codigo);
        if (encontrada.isEmpty()) {
            log.info("Agente rechazado: codigo desconocido");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ValidacionResponse.invalida("Lizentzia-kodea ez da existitzen."));
        }

        Licencia licencia = encontrada.get();

        if (!licencia.isActiva()) {
            log.info("Agente rechazado: licencia desactivada (ordenador={})",
                    licencia.getUsuario().getNombreOrdenador());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ValidacionResponse.invalida("Lizentzia desaktibatuta dago."));
        }

        Usuario usuario = licencia.getUsuario();

        String nombreEnviado = normalizar(peticion == null ? null : peticion.nombreOrdenador());
        if (!nombreEnviado.isBlank()
                && !nombreEnviado.equalsIgnoreCase(usuario.getNombreOrdenador())) {
            log.info("Agente rechazado: ordenador '{}' no coincide con licencia (esperado '{}')",
                    nombreEnviado, usuario.getNombreOrdenador());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ValidacionResponse.invalida("Lizentzia ez dator bat ordenagailu honekin."));
        }

        log.info("Agente autorizado: empresaId={}, ordenador={}",
                usuario.getEmpresa().getId(), usuario.getNombreOrdenador());

        return ResponseEntity.ok(ValidacionResponse.valida(
                usuario.getEmpresa().getId(),
                usuario.getEmpresa().getNombre(),
                usuario.getNombreOrdenador()));
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String normalizarCodigo(String codigo) {
        return normalizar(codigo).replace(" ", "").toUpperCase(Locale.ROOT);
    }

    public record ValidacionRequest(String codigo, String nombreOrdenador) {
    }

    public record ValidacionResponse(
            boolean valida,
            String motivo,
            Long empresaId,
            String empresaNombre,
            String nombreOrdenador) {

        static ValidacionResponse valida(Long empresaId, String empresaNombre, String nombreOrdenador) {
            return new ValidacionResponse(true, null, empresaId, empresaNombre, nombreOrdenador);
        }

        static ValidacionResponse invalida(String motivo) {
            return new ValidacionResponse(false, motivo, null, null, null);
        }
    }
}
