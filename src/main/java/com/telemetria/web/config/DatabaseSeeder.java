package com.telemetria.web.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.telemetria.web.model.Administrador;
import com.telemetria.web.model.Empresa;
import com.telemetria.web.model.Licencia;
import com.telemetria.web.model.Usuario;
import com.telemetria.web.repository.AdministradorRepository;
import com.telemetria.web.repository.EmpresaRepository;
import com.telemetria.web.repository.LicenciaRepository;
import com.telemetria.web.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    CommandLineRunner initDatabase(EmpresaRepository empresaRepo,
                                   AdministradorRepository adminRepo,
                                   UsuarioRepository usuarioRepo,
                                   LicenciaRepository licenciaRepo,
                                   CqlSession cqlSession,
                                   PasswordEncoder passwordEncoder) {
        return args -> {

            if (adminPassword == null || adminPassword.length() < 12) {
                throw new IllegalStateException(
                    "ADMIN_PASSWORD debe tener al menos 12 caracteres. Configura la variable de entorno ADMIN_PASSWORD."
                );
            }

            // ── Crear empresa, admin y usuario de prueba si no existen ──────
            if (empresaRepo.count() == 0) {

                // Empresa
                Empresa empresa = new Empresa();
                empresa.setNombre("Acme Corp");
                empresaRepo.save(empresa);

                // Administrador web (puede iniciar sesion en el panel)
                Administrador admin = new Administrador();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setEmpresa(empresa);
                admin.setSuperAdmin(true);
                adminRepo.save(admin);

                // Usuario de prueba con ordenador (sin acceso web)
                Usuario usuario = new Usuario();
                usuario.setNombre("Usuario Prueba");
                usuario.setNombreOrdenador("PC-TEST");
                usuario.setEmpresa(empresa);
                usuarioRepo.save(usuario);

                // Licencia de prueba para ese usuario
                Licencia licencia = new Licencia();
                licencia.setCodigo("WHM-TEST-TEST-TEST-AABB");
                licencia.setActiva(true);
                licencia.setUsuario(usuario);
                licenciaRepo.save(licencia);

                log.info("====== DATOS DE PRUEBA CREADOS ======");
                log.info("Admin web  -> usuario: admin / contrasena: [ver ADMIN_PASSWORD en .env]");
                log.info("Usuario    -> nombre: Usuario Prueba / ordenador: PC-TEST");
                log.info("Licencia   -> WHM-TEST-TEST-TEST-AABB (activa)");
                log.info("Empresa    -> Acme Corp (id=1)");
                log.info("=====================================");
            }

            // ── Sincronizar empresas de MySQL a Cassandra al arrancar ────────
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
