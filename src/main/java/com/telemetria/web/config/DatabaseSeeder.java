package com.telemetria.web.config;

import com.telemetria.web.model.Administrador;
import com.telemetria.web.model.Empresa;
import com.telemetria.web.repository.AdministradorRepository;
import com.telemetria.web.repository.EmpresaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(EmpresaRepository empresaRepo, AdministradorRepository adminRepo) {
        return args -> {
            if (empresaRepo.count() == 0) {
                // Generamos la primera empresa
                Empresa empresa = new Empresa();
                empresa.setNombre("Acme Corp");
                empresaRepo.save(empresa);

                // Generamos el primer administrador vinculado a esta empresa
                Administrador admin = new Administrador();
                admin.setUsername("admin");
                // Contraseña "test" con formato BCrypt {bcrypt}
                admin.setPassword("{bcrypt}$2b$12$Huz.a4s2smRK1xhHfTANf.eeRf12QMAuWqrwz8janrY8N8vtLU3KC");
                admin.setEmpresa(empresa);
                
                adminRepo.save(admin);
                
                System.out.println("====== USUARIO DE PRUEBA CREADO ======");
                System.out.println("Usuario: admin");
                System.out.println("Contraseña: test");
                System.out.println("Empresa: Acme Corp");
                System.out.println("======================================");
            }
        };
    }
}