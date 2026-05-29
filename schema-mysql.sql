-- ============================================================
-- WebHardMon — Schema MySQL
-- Motor: InnoDB · Charset: utf8mb4
-- Hibernate ddl-auto:update crea estas tablas automaticamente,
-- pero puedes ejecutar este fichero para crear la BD a mano.
-- ============================================================

CREATE DATABASE IF NOT EXISTS telemetriadb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE telemetriadb;

-- ------------------------------------------------------------
-- 1. empresa
--    Organización que agrupa administradores y empleados.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS empresa (
    id     BIGINT       NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 2. administrador
--    Usuario con acceso al panel web (rol ADMIN).
--    Solo estos pueden iniciar sesion en la aplicacion web.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS administrador (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    empresa_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_administrador_username UNIQUE (username),
    CONSTRAINT fk_administrador_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 3. usuario
--    Empleado con un ordenador. NO tiene acceso web.
--    Su ordenador se identifica por nombre_ordenador, que es
--    el mismo valor que se usa en Cassandra como campo 'nombre'
--    en las tablas ordenadores y mediciones.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(100) NOT NULL,
    nombre_ordenador VARCHAR(80)  NOT NULL,
    empresa_id       BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_empresa_ordenador
        UNIQUE (empresa_id, nombre_ordenador),
    CONSTRAINT fk_usuario_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 4. licencia
--    Licencia de ejecucion del agente Go.
--    Relacion 1-1 con usuario: un usuario puede tener como
--    maximo una licencia, y puede no tener ninguna.
--    El campo 'codigo' es el secreto que el agente envia a
--    POST /api/agente/validar para autenticarse.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS licencia (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    codigo         VARCHAR(255) NOT NULL,
    activa         TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_creacion DATETIME     NOT NULL,
    usuario_id     BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_licencia_codigo  UNIQUE (codigo),
    CONSTRAINT uk_licencia_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_licencia_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Datos iniciales de prueba (opcionales)
-- Contrasena del admin: test
-- ------------------------------------------------------------
INSERT IGNORE INTO empresa (id, nombre) VALUES (1, 'Acme Corp');

INSERT IGNORE INTO administrador (id, username, password, empresa_id)
VALUES (1, 'admin',
        '{bcrypt}$2b$12$Huz.a4s2smRK1xhHfTANf.eeRf12QMAuWqrwz8janrY8N8vtLU3KC',
        1);

INSERT IGNORE INTO usuario (id, nombre, nombre_ordenador, empresa_id)
VALUES (1, 'Usuario Prueba', 'PC-TEST', 1);

INSERT IGNORE INTO licencia (id, codigo, activa, fecha_creacion, usuario_id)
VALUES (1, 'WHM-TEST-TEST-TEST-AABB', 1, NOW(), 1);
