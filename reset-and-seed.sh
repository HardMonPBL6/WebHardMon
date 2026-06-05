#!/bin/bash
# reset-and-seed.sh
# Vacia Cassandra y MySQL, y deja MySQL con los datos base que
# necesita seed-random.sh (empresas 1-3, admins, usuarios y licencias).
# Despues lanza seed-random.sh para rellenar Cassandra con telemetria.
#
# Uso: bash reset-and-seed.sh
#
# Contrasenas de los admins (bcrypt de "test"):
#   admin1 / test   -> empresa 1 (Acme Corp)
#   admin2 / test   -> empresa 2 (Beta Industries)
#   admin3 / test   -> empresa 3 (Gamma Solutions)

set -e

CASSANDRA_CONTAINER="webhardmon-cassandra"
MYSQL_CONTAINER="webhardmon-mysql"   # <-- ajusta si tu contenedor tiene otro nombre
MYSQL_DB="telemetriadb"
MYSQL_USER="admin"
MYSQL_PASS="secret"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ─────────────────────────────────────────────
# 1. Comprobar contenedores
# ─────────────────────────────────────────────
for CONTAINER in "$CASSANDRA_CONTAINER" "$MYSQL_CONTAINER"; do
    if ! docker inspect "$CONTAINER" > /dev/null 2>&1; then
        echo "ERROR: El contenedor '$CONTAINER' no existe."
        exit 1
    fi
    STATUS=$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)
    if [ "$STATUS" != "true" ]; then
        echo "ERROR: El contenedor '$CONTAINER' no esta corriendo."
        exit 1
    fi
done

# ─────────────────────────────────────────────
# 2. Esperar a que Cassandra este lista
# ─────────────────────────────────────────────
echo "Esperando a Cassandra..."
until docker exec "$CASSANDRA_CONTAINER" cqlsh -e "describe keyspaces" > /dev/null 2>&1; do
    echo "  ..."
    sleep 3
done
echo "OK: Cassandra lista."

# ─────────────────────────────────────────────
# 3. Vaciar Cassandra
# ─────────────────────────────────────────────
echo ""
echo "Vaciando tablas de Cassandra..."
docker exec -i "$CASSANDRA_CONTAINER" cqlsh << 'CQL'
TRUNCATE webhardmon.mediciones;
TRUNCATE webhardmon.ordenadores;
TRUNCATE webhardmon.empresas;
CQL
echo "OK: Cassandra vaciada."

# ─────────────────────────────────────────────
# 4. Esperar a que MySQL este lista
# ─────────────────────────────────────────────
echo ""
echo "Esperando a MySQL..."
until docker exec "$MYSQL_CONTAINER" \
    mysqladmin ping -u"$MYSQL_USER" -p"$MYSQL_PASS" --silent > /dev/null 2>&1; do
    echo "  ..."
    sleep 3
done
echo "OK: MySQL lista."

# ─────────────────────────────────────────────
# 5. Resetear MySQL
#    - Borra y recrea los datos de prueba para las 3 empresas
#    - Los nombre_ordenador coinciden con los PCs del seed-random.sh
#    - Contrasena bcrypt de "test" para todos los admins
# ─────────────────────────────────────────────
echo ""
echo "Reseteando MySQL..."

docker exec -i "$MYSQL_CONTAINER" \
    mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" "$MYSQL_DB" << 'SQL'
-- ── Deshabilitar FK checks para poder truncar en cualquier orden ──
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE licencia;
TRUNCATE TABLE usuario;
TRUNCATE TABLE administrador;
TRUNCATE TABLE empresa;

SET FOREIGN_KEY_CHECKS = 1;

-- ── Empresas (empresa_id debe coincidir con seed-random.sh) ──
INSERT INTO empresa (id, nombre) VALUES
    (1, 'Acme Corp'),
    (2, 'Beta Industries'),
    (3, 'Gamma Solutions');

-- ── Administradores (password bcrypt de "test") ──
INSERT INTO administrador (id, username, password, empresa_id, super_admin) VALUES
    (1, 'admin1', '{bcrypt}$2b$12$Huz.a4s2smRK1xhHfTANf.eeRf12QMAuWqrwz8janrY8N8vtLU3KC', 1, 0),
    (2, 'admin2', '{bcrypt}$2b$12$Huz.a4s2smRK1xhHfTANf.eeRf12QMAuWqrwz8janrY8N8vtLU3KC', 2, 0),
    (3, 'admin3', '{bcrypt}$2b$12$Huz.a4s2smRK1xhHfTANf.eeRf12QMAuWqrwz8janrY8N8vtLU3KC', 3, 0);

-- ── Usuarios/Ordenadores — empresa 1 (15 PCs: PC-1-1 .. PC-1-15) ──
INSERT INTO usuario (nombre, nombre_ordenador, empresa_id) VALUES
    ('Usuario PC-1-1',  'PC-1-1',  1),
    ('Usuario PC-1-2',  'PC-1-2',  1),
    ('Usuario PC-1-3',  'PC-1-3',  1),
    ('Usuario PC-1-4',  'PC-1-4',  1),
    ('Usuario PC-1-5',  'PC-1-5',  1),
    ('Usuario PC-1-6',  'PC-1-6',  1),
    ('Usuario PC-1-7',  'PC-1-7',  1),
    ('Usuario PC-1-8',  'PC-1-8',  1),
    ('Usuario PC-1-9',  'PC-1-9',  1),
    ('Usuario PC-1-10', 'PC-1-10', 1),
    ('Usuario PC-1-11', 'PC-1-11', 1),
    ('Usuario PC-1-12', 'PC-1-12', 1),
    ('Usuario PC-1-13', 'PC-1-13', 1),
    ('Usuario PC-1-14', 'PC-1-14', 1),
    ('Usuario PC-1-15', 'PC-1-15', 1);

-- ── Usuarios/Ordenadores — empresa 2 (5 PCs) ──
INSERT INTO usuario (nombre, nombre_ordenador, empresa_id) VALUES
    ('Usuario PC-2-1', 'PC-2-1', 2),
    ('Usuario PC-2-2', 'PC-2-2', 2),
    ('Usuario PC-2-3', 'PC-2-3', 2),
    ('Usuario PC-2-4', 'PC-2-4', 2),
    ('Usuario PC-2-5', 'PC-2-5', 2);

-- ── Usuarios/Ordenadores — empresa 3 (3 PCs) ──
INSERT INTO usuario (nombre, nombre_ordenador, empresa_id) VALUES
    ('Usuario PC-3-1', 'PC-3-1', 3),
    ('Usuario PC-3-2', 'PC-3-2', 3),
    ('Usuario PC-3-3', 'PC-3-3', 3);

-- ── Licencias para cada usuario (generadas de ejemplo) ──
INSERT INTO licencia (codigo, activa, fecha_creacion, usuario_id)
SELECT
    CONCAT('WHM-', LPAD(HEX(id * 7919 & 0xFFFF), 4, '0'), '-',
                   LPAD(HEX(id * 6271 & 0xFFFF), 4, '0'), '-',
                   LPAD(HEX(id * 4973 & 0xFFFF), 4, '0'), '-',
                   LPAD(HEX(id * 3491 & 0xFFFF), 4, '0')),
    1,
    NOW(),
    id
FROM usuario;

SELECT 'MySQL reseteado OK' AS resultado;
SQL

echo "OK: MySQL reseteado."

# ─────────────────────────────────────────────
# 6. Lanzar seed-random.sh
# ─────────────────────────────────────────────
echo ""
echo "Lanzando seed-random.sh..."
echo "────────────────────────────────────────────"
bash "$SCRIPT_DIR/seed-random.sh"
