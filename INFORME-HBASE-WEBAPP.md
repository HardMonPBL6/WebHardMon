# Informe Técnico — Integración HBase en la Webapp Spring Boot

## Sección "Tendencias historikoak" en el dashboard de empresa

---

## 1. Qué se añadió y por qué

El dashboard de empresa ya mostraba datos **en tiempo real** de Cassandra a través de Grafana. HBase aporta algo distinto: **agregados horarios pre-calculados** (avg / min / max / count) para los últimos 90 días, producidos por el job MapReduce. Sin esta integración, esos datos existían en HBase pero no eran visibles en ningún sitio.

La nueva sección muestra **tendencias históricas a nivel de flota** (empresa + todas las tiers de hardware combinadas) en la misma pantalla del dashboard de empresa, sin reemplazar ni interferir con Grafana.

---

## 2. Arquitectura del flujo

```
MapReduce (cada hora)
    └─► HBase webhardmon_hourly (GCP-B, REST :8085)
                │
                │  HTTP GET scanner (RestTemplate, timeout 15s)
                ▼
        HBaseService.java
                │
                │  List<HourlyAggregateDTO>
                ▼
        GET /api/historico?days=N  (WebController)
                │
                │  JSON
                ▼
        Browser JS (fetch + renderHistorico)
                │
                ▼
        Tabla HTML en layout.html (sección "Tendencias historikoak")
```

---

## 3. Ficheros modificados

### 3.1 `src/main/resources/application.yml`

Añadidas dos propiedades bajo `app:`:

```yaml
app:
  hbase:
    rest-url: ${HBASE_REST_URL:http://10.0.0.30:8085}
    table: ${HBASE_TABLE:webhardmon_hourly}
```

Ambas son sobreescribibles por variables de entorno, con los valores de producción como default.

---

### 3.2 `src/main/java/com/telemetria/web/model/HourlyAggregateDTO.java` *(nuevo)*

Record Java que representa un slot de datos agregados (una hora o un día). Todos los campos corresponden directamente a columnas de HBase (snake_case → camelCase):

| Campo Java | Columna HBase | Descripción |
|---|---|---|
| `slot` | row key (parte `yyyyMMddHH`) | Ventana temporal |
| `cpuAvg` / `cpuMin` / `cpuMax` | `m:cpu_avg` / `m:cpu_min` / `m:cpu_max` | % CPU |
| `ramAvg` / `ramMin` / `ramMax` | `m:ram_avg` / `m:ram_min` / `m:ram_max` | % RAM |
| `stoAvg` / `stoMin` / `stoMax` | `m:sto_avg` / `m:sto_min` / `m:sto_max` | % disco |
| `batAvg` / `batMin` / `batMax` | `m:bat_avg` / `m:bat_min` / `m:bat_max` | % batería |
| `tmpAvg` / `tmpMin` / `tmpMax` | `m:tmp_avg` / `m:tmp_min` / `m:tmp_max` | Temperatura (°C) |
| `strAvg` / `strMin` / `strMax` | `m:str_avg` / `m:str_min` / `m:str_max` | Stress score |
| `count` | `m:count` | Nº de muestras del slot |

Jackson serializa este record a JSON automáticamente (Spring Boot 3.2 + Jackson 2.14+).

---

### 3.3 `src/main/java/com/telemetria/web/service/HBaseService.java` *(nuevo)*

Servicio que consulta la **HBase REST API** (`http://10.0.0.30:8085`) con `RestTemplate`. Sin dependencias nuevas en `pom.xml` (RestTemplate y Jackson ya estaban).

**Configuración de RestTemplate:**
- Connect timeout: 3 segundos
- Read timeout: 15 segundos
- Si HBase no es alcanzable, devuelve `List.of()` sin lanzar excepción

**Flujo de consulta:**

1. **Prefix scan** sobre la tabla `webhardmon_hourly`:
   - `startRow = "{empresaId}|"`
   - `endRow = "{empresaId}}"` (`}` = char 125 = `|` + 1, corta el scan limpiamente)
   - Devuelve todas las filas de esa empresa sin importar tier de hardware

2. **Filtrado en memoria** por rango de fecha (`fromSlot = now - N días`)

3. **Decodificación de celdas**:
   - Row key y nombre de columna: Base64 → UTF-8 string
   - Valores de métricas: Base64 → 8 bytes big-endian → `double` via `ByteBuffer`
   - Contador (`m:count`): Base64 → 8 bytes big-endian → `long` via `ByteBuffer`

4. **Agregación cross-tier**: los datos se agrupan por slot horario (ignorando tier de hardware). Para cada slot:
   - `avg`: promedio ponderado por `count` de cada tier
   - `min`: mínimo global entre todos los tiers
   - `max`: máximo global entre todos los tiers
   - `count`: suma de counts de todos los tiers

**Por qué promedio ponderado:** si hay 5 equipos de 8 GB y 20 de 16 GB en la misma hora, el dato de los 8 GB no debe tener el mismo peso que el de los 16 GB.

**Límite:** máximo 30 días consultables (cap en `getRecentAggregates`).

---

### 3.4 `src/main/java/com/telemetria/web/controller/WebController.java`

Cambios:
- Inyección de `HBaseService` en el constructor
- Nuevo endpoint REST:

```
GET /api/historico?days={1|7|30}
```

- Requiere autenticación (el `empresaId` viene del `AdminPrincipal` del contexto de seguridad, nunca del request)
- Devuelve `List<HourlyAggregateDTO>` como JSON
- El parámetro `days` acepta 1, 7 o 30 (validado y capado en el servicio)

---

### 3.5 `src/main/resources/templates/layout.html`

**HTML añadido** (visible únicamente cuando `dashboardTipo == 'empresa'`):

- Bloque con título "Tendencias historikoak" y subtítulo explicativo
- Selector de rango: Azken 24 orduak / Azken 7 egunak / Azken 30 egunak (default: 7 egunak)
- Div `#historico-loading` (indicador de carga)
- Div `#historico-table-container` (destino de la tabla renderizada por JS)

**JavaScript añadido** en el bloque `<script th:inline="javascript">` existente:

| Función | Rol |
|---|---|
| `loadHistorico(days)` | `fetch /api/historico?days=N`, llama a `renderHistorico` |
| `renderHistorico(data, days)` | Decide si mostrar vista horaria o diaria, genera HTML de tabla |
| `groupHistoricoByDay(data)` | Agrupa slots horarios por día: promedio ponderado para avg, `Math.min` para min, `Math.max` para max |
| `fmtSlot(slot, isHourly)` | Formatea `yyyyMMddHH` → `yyyy/MM/dd HH:00` o `yyyy/MM/dd` |
| `fmtVal(v)` | Formatea número con 1 decimal, `—` si nulo |
| `mColor(v, warn, crit)` | Devuelve clase Tailwind de color según umbrales |
| `td(v, warn, crit, unit)` | Celda avg con color |
| `tdN(v, unit)` | Celda min/max en gris |
| `tdNB(v, unit)` | Celda min/max en gris con borde derecho (separa grupos) |

**Estructura de la tabla** (cabeceras en dos filas):

```
         CPU (%)          RAM (%)         Diskoa (%)      Tenp. (°C)      Bateria (%)      Estresa
Slot   avg  min  max │ avg  min  max │ avg  min  max │ avg  min  max │ avg  min  max │ avg  min  max │ Gailuak
```

- Las columnas `avg` se colorean (verde/amarillo/rojo según umbrales)
- Las columnas `min` y `max` aparecen en gris (son extremos puntuales, el color relevante es el promedio)
- Separador visual (`border-r`) entre cada grupo de métrica

**Umbrales de color para avg:**

| Métrica | Amarillo | Rojo |
|---|---|---|
| CPU | ≥ 60% | ≥ 80% |
| RAM | ≥ 60% | ≥ 80% |
| Disco | ≥ 70% | ≥ 90% |
| Temperatura | ≥ 65°C | ≥ 80°C |
| Stress | ≥ 40 | ≥ 70 |

**Vista horaria (24h):** 24 filas, una por hora.
**Vista diaria (7d / 30d):** agrupación por día en el browser, una fila por día.

**Carga inicial:** `loadHistorico(7)` se llama en `DOMContentLoaded` cuando `dashboardTipo === 'empresa'`.

---

## 4. Degradación graceful

Si HBase no está disponible (VMs Spot apagadas, WireGuard caído, etc.):
- `createScanner` captura la excepción y devuelve `null` → `getRecentAggregates` devuelve `List.of()`
- El endpoint devuelve `[]`
- El frontend muestra: *"Ez dago datu historikorik aukeratutako tartean."*
- El resto del dashboard (Grafana, Cassandra) no se ve afectado

---

## 5. Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `HBASE_REST_URL` | `http://10.0.0.30:8085` | URL de la HBase REST API (node-01 GCP-B) |
| `HBASE_TABLE` | `webhardmon_hourly` | Tabla HBase de agregados |

---

## 6. Datos que NO muestra esta sección

- Métricas por ordenador individual → Cassandra + Grafana (dashboard Ordenagailuak)
- Datos de la última hora → pueden no existir aún en HBase (cron cada :10)
- Comparativa entre tiers de hardware → los datos se mezclan en un único agregado por hora
- Los filtros de cabecera (RAM, Diskoa, Top, Ordena) no afectan a esta sección; son exclusivos de los iframes de Grafana
