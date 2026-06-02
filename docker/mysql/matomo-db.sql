-- Base de datos para Matomo Analytics
CREATE DATABASE IF NOT EXISTS matomodb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- El usuario 'admin' ya existe (creado por MYSQL_USER), solo necesita permisos sobre matomodb
GRANT ALL PRIVILEGES ON matomodb.* TO 'admin'@'%';
FLUSH PRIVILEGES;
