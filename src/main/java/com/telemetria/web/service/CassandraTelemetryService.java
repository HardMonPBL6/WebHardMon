package com.telemetria.web.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CassandraTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(CassandraTelemetryService.class);

    private final CqlSession cqlSession;

    public CassandraTelemetryService(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    public List<String> findNombresByEmpresa(Long empresaId) {
        if (empresaId == null) {
            return Collections.emptyList();
        }
        try {
            List<String> nombres = new ArrayList<>();
            for (Row row : cqlSession.execute(
                    "SELECT nombre FROM webhardmon.ordenadores WHERE empresa_id = ?",
                    empresaId)) {
                String nombre = row.getString("nombre");
                if (nombre != null && !nombre.isBlank()) {
                    nombres.add(nombre);
                }
            }
            nombres.sort(String::compareToIgnoreCase);
            return nombres;
        } catch (Exception e) {
            log.warn("No se pudo consultar Cassandra para empresaId={}: {}", empresaId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
