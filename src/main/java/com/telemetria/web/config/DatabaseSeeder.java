package com.telemetria.web.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.telemetria.web.model.Administrador;
import com.telemetria.web.model.Empresa;
import com.telemetria.web.repository.AdministradorRepository;
import com.telemetria.web.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    @Bean
    CommandLineRunner initDatabase(EmpresaRepository empresaRepo,
                                   AdministradorRepository adminRepo,
                                   CqlSession cqlSession) {
        return args -> {
            // Crear empresa y admin por defecto si no existen
            if (empresaRepo.count() == 0) {
                Empresa empresa = new Empresa();
                empresa.setNombre("Acme Corp");
                empresaRepo.save(empresa);

                Administrador admin = new Administrador();
                admin.setUsername("admin");
                admin.setPassword("{bcrypt}$2b$12$Huz.a4s2smRK1xhHfTANf.eeRf12QMAuWqrwz8janrY8N8vtLU3KC");
                admin.setEmpresa(empresa);
                adminRepo.save(admin);

                log.info("====== USUARIO DE PRUEBA CREADO ======");
                log.info("Erabiltzailea: admin");
                log.info("Pasahitza: test");
                log.info("Empresa: Acme Corp");
                log.info("======================================");
            }

            // Sincronizar todas las empresas de MySQL a Cassandra al arrancar
            empresaRepo.findAll().forEach(emp -> {
                try {
                    cqlSession.execute(
                        "INSERT INTO webhardmon.empresas (empresa_id, nombre) VALUES (?, ?)",
                        emp.getId(), emp.getNombre()
                    );
                    log.debug("Empresa {} sincronizada a Cassandra", emp.getId());
                } catch (Exception e) {
                    log.warn("No se pudo sincronizar empresa {} a Cassandra: {}", emp.getId(), e.getMessage());
                }
            });
        };
    }
}
