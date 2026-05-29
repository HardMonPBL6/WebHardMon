-- Migracion desde el schema antiguo al nuevo
-- Ejecutar con: docker exec -i webhardmon-cassandra cqlsh < migrate-cassandra.cql

-- Eliminar tablas antiguas
DROP TABLE IF EXISTS webhardmon.portatiles_by_empresa;
DROP TABLE IF EXISTS webhardmon.telemetry_by_empresa;

-- Crear nuevas tablas
CREATE TABLE IF NOT EXISTS webhardmon.empresas (
    empresa_id bigint,
    nombre     text,
    PRIMARY KEY (empresa_id)
);

CREATE TABLE IF NOT EXISTS webhardmon.ordenadores (
    empresa_id    bigint,
    nombre        text,
    procesador    text,
    ram           text,
    almacenamiento text,
    ultima_vez    timestamp,
    PRIMARY KEY ((empresa_id), nombre)
);

CREATE TABLE IF NOT EXISTS webhardmon.mediciones (
    empresa_id     bigint,
    nombre         text,
    ts             timestamp,
    cpu_percent    double,
    ram_percent    double,
    disco_percent  double,
    temperatura    double,
    bateria_percent double,
    stress_score   double,
    ram            text,
    almacenamiento text,
    PRIMARY KEY ((empresa_id), ts, nombre)
) WITH CLUSTERING ORDER BY (ts DESC, nombre ASC);
