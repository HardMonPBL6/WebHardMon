#!/bin/bash

VM_URL="http://localhost:8428/api/v1/import/prometheus"

echo "Insertando datos de prueba en VictoriaMetrics..."

for i in {1..100}
do
  TIMESTAMP=$(($(date +%s) * 1000))

  EMPRESA_ID=$(( (i % 3) + 1 ))
  PORTATIL_ID="PC-$EMPRESA_ID-$i"

  CPU=$(( RANDOM % 100 ))
  RAM=$(( 30 + RANDOM % 70 ))
  DISK=$(( 20 + RANDOM % 80 ))
  TEMP=$(( 35 + RANDOM % 55 ))
  BATTERY=$(( RANDOM % 100 ))

  cat <<EOF | curl -s -X POST "$VM_URL" --data-binary @-
cpu_usage_percent{empresa="$EMPRESA_ID",portatil="$PORTATIL_ID"} $CPU $TIMESTAMP
ram_usage_percent{empresa="$EMPRESA_ID",portatil="$PORTATIL_ID"} $RAM $TIMESTAMP
disk_usage_percent{empresa="$EMPRESA_ID",portatil="$PORTATIL_ID"} $DISK $TIMESTAMP
temperature_celsius{empresa="$EMPRESA_ID",portatil="$PORTATIL_ID"} $TEMP $TIMESTAMP
battery_percent{empresa="$EMPRESA_ID",portatil="$PORTATIL_ID"} $BATTERY $TIMESTAMP
EOF

  sleep 1
done

echo "Datos insertados correctamente."