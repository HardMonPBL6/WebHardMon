#!/bin/bash
set -e

# Arranca Cassandra en background usando el entrypoint oficial
/usr/local/bin/docker-entrypoint.sh cassandra -f &
CASS_PID=$!

echo "[init] Esperando a que Cassandra acepte conexiones..."
until cqlsh localhost -e "describe keyspaces" >/dev/null 2>&1; do
    sleep 5
    echo "[init] Cassandra aún no está lista, reintentando..."
done

echo "[init] Aplicando schema..."
cqlsh localhost -f /init/schema.cql

echo "[init] Aplicando datos de seed..."
cqlsh localhost -f /init/seed.cql

echo "[init] Inicialización completa."

# Vuelve al proceso principal de Cassandra (foreground)
wait $CASS_PID
