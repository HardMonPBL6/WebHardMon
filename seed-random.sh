#!/bin/bash
# seed-random.sh
# Genera e inserta datos aleatorios de telemetria en Cassandra para testing.
# Crea 3 empresas con ordenadores, con datos de las ultimas 48h.
# Cada ordenador lleva un codigo_licencia de ejemplo para simular
# el registro del agente Go.
#
# Uso: bash seed-random.sh

set -e

CONTAINER="webhardmon-cassandra"

if ! docker inspect "$CONTAINER" > /dev/null 2>&1; then
    echo "ERROR: El contenedor '$CONTAINER' no existe."
    exit 1
fi

STATUS=$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)
if [ "$STATUS" != "true" ]; then
    echo "ERROR: El contenedor '$CONTAINER' no esta corriendo."
    exit 1
fi

echo "Conectando a Cassandra..."
until docker exec "$CONTAINER" cqlsh -e "describe keyspaces" > /dev/null 2>&1; do
    echo "  Esperando a que Cassandra este lista..."
    sleep 3
done
echo "OK: Cassandra lista."
echo ""

python3 - << 'PYEOF'
import random, subprocess, sys
from datetime import datetime, timedelta, timezone

KEYSPACE   = "webhardmon"
CONTAINER  = "webhardmon-cassandra"
INTERVAL   = timedelta(minutes=15)
HOURS_BACK = 48
POINTS     = HOURS_BACK * 4  # 192 puntos por ordenador

PROCESADORES = [
    "Intel i5-1135G7", "Intel i7-1165G7", "AMD Ryzen 5 5600U",
    "AMD Ryzen 7 5800U", "Apple M1", "Intel i3-1115G4", "AMD Ryzen 3 5300U"
]
RAMS            = ["8GB", "16GB", "32GB", "64GB"]
ALMACENAMIENTOS = ["256GB SSD", "512GB SSD", "1TB SSD", "2TB SSD"]
LICENSE_CHARS   = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

def gen_license():
    """Genera un codigo de licencia de ejemplo (formato WHM-XXXX-XXXX-XXXX-XXXX)."""
    bloques = ["".join(random.choices(LICENSE_CHARS, k=4)) for _ in range(4)]
    return "WHM-" + "-".join(bloques)

# Empresas y sus ordenadores (empresa_id debe coincidir con MySQL)
EMPRESAS = {
    1: [f"PC-1-{i}" for i in range(1, 16)],  # 15 ordenadores para probar top 5/10/15
    2: ["PC-2-1", "PC-2-2", "PC-2-3", "PC-2-4", "PC-2-5"],
    3: ["PC-3-1", "PC-3-2", "PC-3-3"],
}

NOMBRES_EMPRESA = {
    1: "Acme Corp",
    2: "Beta Industries",
    3: "Gamma Solutions",
}

NOW = datetime.now(timezone.utc)
cql_lines = []
total_pcs = sum(len(v) for v in EMPRESAS.values())

print(f"Generando datos para {total_pcs} ordenadores x {POINTS} puntos = {total_pcs * POINTS} registros...")
print(f"Periodo: ultimas {HOURS_BACK} horas (intervalos de 15 min)")
print()

# Insertar empresas en Cassandra
for emp_id, emp_nombre in NOMBRES_EMPRESA.items():
    cql_lines.append(
        f"INSERT INTO {KEYSPACE}.empresas (empresa_id, nombre) VALUES ({emp_id}, '{emp_nombre}');"
    )

for empresa_id, ordenadores in EMPRESAS.items():
    print(f"  Empresa {empresa_id}: {len(ordenadores)} ordenadores -> {ordenadores}")
    for nombre in ordenadores:
        proc    = random.choice(PROCESADORES)
        ram     = random.choice(RAMS)
        alm     = random.choice(ALMACENAMIENTOS)
        codigo  = gen_license()

        ts_now = int(NOW.timestamp() * 1000)
        cql_lines.append(
            f"INSERT INTO {KEYSPACE}.ordenadores "
            f"(empresa_id, nombre, procesador, ram, almacenamiento, ultima_vez, codigo_licencia) "
            f"VALUES ({empresa_id}, '{nombre}', '{proc}', '{ram}', '{alm}', {ts_now}, '{codigo}');"
        )

        # Valores base por ordenador con perfil propio
        base_cpu     = random.uniform(15, 65)
        base_ram     = random.uniform(25, 75)
        base_disco   = random.uniform(30, 70)
        base_temp    = random.uniform(38, 62)
        base_bateria = random.uniform(30, 95)

        for i in range(POINTS):
            ts = int((NOW - INTERVAL * i).timestamp() * 1000)

            cpu     = max(1.0,  min(99.9, base_cpu     + random.gauss(0, 12)))
            ram_pct = max(1.0,  min(99.9, base_ram     + random.gauss(0, 8)))
            disco   = max(1.0,  min(99.9, base_disco   + random.gauss(0, 2)))
            temp    = max(28.0, min(95.0, base_temp    + random.gauss(0, 6) + cpu * 0.15))
            bateria = max(1.0,  min(100.0, base_bateria - i * 0.04 + random.gauss(0, 4)))
            stress  = max(0.1,  min(99.9, (cpu + ram_pct) / 2.0 + random.gauss(0, 6)))

            cql_lines.append(
                f"INSERT INTO {KEYSPACE}.mediciones "
                f"(empresa_id, nombre, ts, cpu_percent, ram_percent, disco_percent, "
                f"temperatura, bateria_percent, stress_score, ram, almacenamiento) "
                f"VALUES ({empresa_id}, '{nombre}', {ts}, "
                f"{cpu:.1f}, {ram_pct:.1f}, {disco:.1f}, {temp:.1f}, "
                f"{bateria:.1f}, {stress:.1f}, '{ram}', '{alm}');"
            )

cql_script = "\n".join(cql_lines)
total = len(cql_lines)

print()
print(f"Insertando {total} registros en Cassandra (puede tardar ~30 seg)...")

result = subprocess.run(
    ["docker", "exec", "-i", CONTAINER, "cqlsh"],
    input=cql_script,
    capture_output=True,
    text=True,
    timeout=180
)

if result.returncode == 0:
    print()
    print("=" * 50)
    print("OK: Datos insertados correctamente.")
    print(f"    {total_pcs} ordenadores, {POINTS} puntos cada uno")
    print(f"    Total registros: {total}")
    print("=" * 50)
    print()
    print("Abre Grafana en http://localhost:3000")
    print("Rango de tiempo recomendado: 'Last 2 days'")
else:
    print()
    print("ERROR al insertar datos:")
    print(result.stderr[:1000])
    sys.exit(1)

PYEOF
