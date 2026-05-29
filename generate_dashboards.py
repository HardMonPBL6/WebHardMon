import json

def ds():
    return {"type": "hadesarchitect-cassandra-datasource", "uid": "cassandra"}

def tgt(cql, fmt="table"):
    return {"refId": "A", "datasource": ds(), "target": cql,
            "rawQuery": True, "queryType": "query", "format": fmt}

def thresh(steps):
    return {"mode": "absolute", "steps": steps}

GOR   = [{"color":"green","value":None},{"color":"orange","value":70},{"color":"red","value":90}]
TBAT  = [{"color":"red","value":None},{"color":"orange","value":20},{"color":"green","value":50}]
TTEMP = [{"color":"green","value":None},{"color":"orange","value":65},{"color":"red","value":80}]
TDISK = [{"color":"green","value":None},{"color":"orange","value":75},{"color":"red","value":90}]

# ─────────────────────────────────────────────────────────────────────────────
# DASHBOARD 1  — Enpresaren panela (empresa)
# ─────────────────────────────────────────────────────────────────────────────
empresa = {
    "annotations": {"list": []}, "editable": True, "fiscalYearStartMonth": 0,
    "graphTooltip": 1, "id": None,
    "uid": "webhardmon-empresa",
    "title": "WebHardMon - Enpresaren panela",
    "tags": ["webhardmon", "cassandra"],
    "timezone": "browser", "schemaVersion": 41, "version": 1,
    "refresh": "10s", "time": {"from": "now-30d", "to": "now"},
    "templating": {"list": [
        {"name": "empresa", "type": "constant", "query": "1",
         "current": {"value": "1", "text": "1"}, "hide": 2,
         "label": "Enpresa", "skipUrlSync": False},
        {"name": "topk", "type": "custom", "query": "5,10,15",
         "current": {"selected": True, "text": "10", "value": "10"},
         "options": [{"selected": False, "text": "5", "value": "5"},
                     {"selected": True,  "text": "10", "value": "10"},
                     {"selected": False, "text": "15", "value": "15"}],
         "hide": 0, "label": "Top", "skipUrlSync": False},
        {"name": "ram", "type": "custom",
         "query": ".*,8GB,16GB,32GB,64GB",
         "current": {"selected": True, "text": "Guztiak", "value": ".*"},
         "options": [{"selected": True,  "text": "Guztiak", "value": ".*"},
                     {"selected": False, "text": "8 GB",    "value": "8GB"},
                     {"selected": False, "text": "16 GB",   "value": "16GB"},
                     {"selected": False, "text": "32 GB",   "value": "32GB"},
                     {"selected": False, "text": "64 GB",   "value": "64GB"}],
         "hide": 2, "label": "RAM", "skipUrlSync": False},
        {"name": "storage", "type": "custom",
         "query": ".*,256GB SSD,512GB SSD,1TB SSD,2TB SSD",
         "current": {"selected": True, "text": "Guztiak", "value": ".*"},
         "options": [{"selected": True,  "text": "Guztiak",     "value": ".*"},
                     {"selected": False, "text": "256 GB SSD",  "value": "256GB SSD"},
                     {"selected": False, "text": "512 GB SSD",  "value": "512GB SSD"},
                     {"selected": False, "text": "1 TB SSD",    "value": "1TB SSD"},
                     {"selected": False, "text": "2 TB SSD",    "value": "2TB SSD"}],
         "hide": 2, "label": "Diskoa", "skipUrlSync": False},
    ]},
    "panels": []
}
p = empresa["panels"]

# ── KPI stats row (averages) ──────────────────────────────────────────────────
for pid, x, title, unit, col, lbl, steps, typ in [
    (1, 0,  "CPU",        "percent", "AVG(cpu_percent)",    "CPU %",       GOR,   "stat"),
    (2, 4,  "RAM",        "percent", "AVG(ram_percent)",    "RAM %",       GOR,   "stat"),
    (3, 8,  "Diskoa",     "percent", "AVG(disco_percent)",  "Diskoa %",    TDISK, "stat"),
    (4, 12, "Tenperatura","celsius", "AVG(temperatura)",    "Tenperatura", TTEMP, "stat"),
    (5, 16, "Bateria",    "percent", "AVG(bateria_percent)","Bateria %",   TBAT,  "gauge"),
    (6, 20, "Estresa",    "short",   "AVG(stress_score)",   "Estresa",     GOR,   "stat"),
]:
    cassandra_col = "system." + col.lower()
    cql = (f"SELECT empresa_id, {col} FROM mediciones "
           f"WHERE empresa_id = $empresa AND ts > $__timeFrom AND ts < $__timeTo "
           f"GROUP BY empresa_id ALLOW FILTERING")
    panel = {
        "id": pid, "type": typ, "title": title,
        "gridPos": {"x": x, "y": 0, "w": 4, "h": 4}, "datasource": ds(),
        "targets": [tgt(cql)],
        "transformations": [{"id": "organize", "options": {
            "excludeByName": {"empresa_id": True},
            "renameByName": {cassandra_col: lbl}
        }}],
        "fieldConfig": {"defaults": {"unit": unit, "thresholds": thresh(steps)}, "overrides": []},
    }
    if typ == "stat":
        panel["options"] = {"reduceOptions": {"values": False, "calcs": ["lastNotNull"], "fields": ""},
                            "orientation": "auto", "textMode": "auto", "colorMode": "value",
                            "graphMode": "area", "justifyMode": "auto"}
    else:
        panel["fieldConfig"]["defaults"].update({"min": 0, "max": 100})
        panel["options"] = {"reduceOptions": {"values": False, "calcs": ["lastNotNull"], "fields": ""},
                            "orientation": "auto", "showThresholdLabels": False, "showThresholdMarkers": True}
    p.append(panel)

# ── Count of active computers ─────────────────────────────────────────────────
p.append({
    "id": 7, "type": "stat", "title": "Ordenagailuak",
    "gridPos": {"x": 0, "y": 0, "w": 4, "h": 4},   # placed after shifting panels manually
    "datasource": ds(),
    "targets": [tgt("SELECT COUNT(*) FROM ordenadores WHERE empresa_id = $empresa")],
    "transformations": [{"id": "organize", "options": {"renameByName": {"count": "Aktiboak"}}}],
    "fieldConfig": {"defaults": {"unit": "short", "thresholds": thresh([{"color":"green","value":None}])}, "overrides": []},
    "options": {"reduceOptions": {"values": False, "calcs": ["lastNotNull"], "fields": ""},
                "orientation": "auto", "textMode": "auto", "colorMode": "value",
                "graphMode": "none", "justifyMode": "auto"}
})
# fix positions: KPIs 4 wide each across 24 cols → 6 panels fit exactly
for i, panel in enumerate(p[:6]):
    panel["gridPos"] = {"x": i * 4, "y": 0, "w": 4, "h": 4}
# ordenagailuak doesn't fit in row 0 with 6 KPIs already — make it row 1 standalone or remove
# Actually we have exactly 6 KPIs (cpu, ram, disco, temp, bateria, estresa) which fill 24 cols.
# Remove the redundant ordenagailuak panel (count is visible from the rankings)
p.pop()  # remove the count panel we just added

# ── Ranking tables (worst performers, filtered by RAM / storage) ──────────────
# Query includes ram + almacenamiento so filterByValue can apply them.
# Pipeline: merge → groupBy(nombre+ram+alm, mean metric) → filterByValue × 2 → sortBy desc → limit topk → organize
RANKINGS = [
    (7,  4,  "CPU gehien erabili dutenak",       "percent", "cpu_percent",    "CPU %",       GOR),
    (8,  14, "RAM gehien erabili dutenak",        "percent", "ram_percent",    "RAM %",       GOR),
    (9,  24, "Diskoa gehien erabili dutenak",     "percent", "disco_percent",  "Diskoa %",    TDISK),
    (10, 34, "Tenperatura altuena dutenak",       "celsius", "temperatura",    "Tenperatura", TTEMP),
    (11, 44, "Stress puntuazio altuena dutenak",  "short",   "stress_score",   "Estresa",     GOR),
]
for pid, y, title, unit, col, lbl, steps in RANKINGS:
    cql = (f"SELECT nombre, {col}, ram, almacenamiento, ts "
           f"FROM mediciones WHERE empresa_id = $empresa "
           f"AND ts > $__timeFrom AND ts < $__timeTo ALLOW FILTERING")
    mean_name = f"{col} (mean)"
    p.append({
        "id": pid, "type": "table", "title": title,
        "description": "RAM eta biltegiratze iragazkia aplikatuta. Handitik txikira ordenatuta.",
        "gridPos": {"x": 0, "y": y, "w": 24, "h": 10},
        "datasource": ds(),
        "targets": [tgt(cql, fmt="time_series")],
        "transformations": [
            {"id": "merge", "options": {}},
            {"id": "groupBy", "options": {"fields": {
                "nombre":         {"aggregations": [],       "operation": "groupby"},
                "ram":            {"aggregations": [],       "operation": "groupby"},
                "almacenamiento": {"aggregations": [],       "operation": "groupby"},
                col:              {"aggregations": ["mean"], "operation": "aggregate"},
            }}},
            {"id": "filterByValue", "options": {
                "filters": [{"fieldName": "ram",
                             "config": {"id": "regex", "options": {"value": "${ram}"}}}],
                "type": "include", "match": "all"
            }},
            {"id": "filterByValue", "options": {
                "filters": [{"fieldName": "almacenamiento",
                             "config": {"id": "regex", "options": {"value": "${storage}"}}}],
                "type": "include", "match": "all"
            }},
            {"id": "sortBy", "options": {"fields": [{"displayName": mean_name, "desc": True}]}},
            {"id": "limit",  "options": {"limitField": 15}},
            {"id": "organize", "options": {
                "excludeByName": {"ts": True, "ram": True, "almacenamiento": True},
                "renameByName": {"nombre": "Ordenagailua", mean_name: lbl}
            }},
        ],
        "fieldConfig": {
            "defaults": {
                "unit": unit, "decimals": 1,
                "thresholds": thresh(steps),
                "custom": {"align": "auto", "cellOptions": {"type": "auto"}},
            },
            "overrides": [
                {"matcher": {"id": "byName", "options": "Ordenagailua"},
                 "properties": [{"id": "custom.width", "value": 320},
                                {"id": "unit",         "value": "string"},
                                {"id": "custom.align", "value": "left"}]},
                {"matcher": {"id": "byName", "options": lbl},
                 "properties": [{"id": "custom.width",       "value": 520},
                                {"id": "custom.align",       "value": "left"},
                                {"id": "custom.cellOptions", "value": {"type": "gauge", "mode": "gradient", "valueDisplayMode": "text"}},
                                {"id": "unit",               "value": unit},
                                {"id": "decimals",           "value": 1},
                                {"id": "min",                "value": 0},
                                {"id": "max",                "value": 100}]},
            ],
        },
        "options": {"showHeader": True, "cellHeight": "md", "footer": {"show": False}},
    })

# ─────────────────────────────────────────────────────────────────────────────
# DASHBOARD 2  — Ordenagailuaren panela (ordenador individual)
# ─────────────────────────────────────────────────────────────────────────────
ordenador = {
    "annotations": {"list": []}, "editable": True, "fiscalYearStartMonth": 0,
    "graphTooltip": 1, "id": None,
    "uid": "webhardmon-ordenadores",
    "title": "WebHardMon - Ordenagailuaren panela",
    "tags": ["webhardmon", "cassandra"],
    "timezone": "browser", "schemaVersion": 41, "version": 1,
    "refresh": "5s", "time": {"from": "now-24h", "to": "now"},
    "templating": {"list": [
        {"name": "empresa", "type": "constant", "query": "1",
         "current": {"value": "1", "text": "1"}, "hide": 2,
         "label": "Enpresa", "skipUrlSync": False},
        {"name": "nombre", "type": "textbox", "query": "PC-1-1",
         "current": {"value": "PC-1-1", "text": "PC-1-1"}, "hide": 2,
         "label": "Ordenagailua", "skipUrlSync": False},
    ]},
    "panels": []
}
op = ordenador["panels"]

# ── Fila KPIs: último valor de cada métrica ───────────────────────────────────
KPI_METRICS = [
    (1, 0,  "CPU",        "percent", "cpu_percent",     "CPU %",       GOR,   "stat"),
    (2, 4,  "RAM",        "percent", "ram_percent",      "RAM %",       GOR,   "stat"),
    (3, 8,  "Diskoa",     "percent", "disco_percent",    "Diskoa %",    TDISK, "stat"),
    (4, 12, "Tenperatura","celsius", "temperatura",      "Tenperatura", TTEMP, "stat"),
    (5, 16, "Bateria",    "percent", "bateria_percent",  "Bateria %",   TBAT,  "gauge"),
    (6, 20, "Estresa",    "short",   "stress_score",     "Estresa",     GOR,   "stat"),
]
for pid, x, title, unit, col, lbl, steps, typ in KPI_METRICS:
    cql = (f"SELECT nombre, {col}, ts FROM mediciones "
           f"WHERE empresa_id = $empresa AND nombre = '$nombre' "
           f"AND ts > $__timeFrom AND ts < $__timeTo ALLOW FILTERING")
    panel = {
        "id": pid, "type": typ, "title": title,
        "gridPos": {"x": x, "y": 0, "w": 4, "h": 4}, "datasource": ds(),
        "targets": [tgt(cql, fmt="time_series")],
        "transformations": [{"id": "organize", "options": {
            "excludeByName": {"nombre": True, "ts": True},
            "renameByName": {col: lbl}
        }}],
        "fieldConfig": {"defaults": {"unit": unit, "thresholds": thresh(steps)}, "overrides": []},
    }
    if typ == "stat":
        panel["options"] = {"reduceOptions": {"values": False, "calcs": ["lastNotNull"], "fields": ""},
                            "orientation": "auto", "textMode": "auto", "colorMode": "value",
                            "graphMode": "area", "justifyMode": "auto"}
    else:
        panel["fieldConfig"]["defaults"].update({"min": 0, "max": 100})
        panel["options"] = {"reduceOptions": {"values": False, "calcs": ["lastNotNull"], "fields": ""},
                            "orientation": "auto", "showThresholdLabels": False, "showThresholdMarkers": True}
    op.append(panel)

# ── Historial: 6 gráficas de series temporales (IDs 7-12, coinciden con el HTML) ─
TS_PANELS = [
    (7,  0, 4,  "CPU denboran",        "percent", "cpu_percent",    "CPU %",       GOR),
    (8,  12,4,  "RAM denboran",        "percent", "ram_percent",    "RAM %",       GOR),
    (9,  0, 12, "Diskoa denboran",     "percent", "disco_percent",  "Diskoa %",    TDISK),
    (10, 12,12, "Tenperatura denboran","celsius", "temperatura",    "Tenperatura", TTEMP),
    (11, 0, 20, "Bateria denboran",    "percent", "bateria_percent","Bateria %",   TBAT),
    (12, 12,20, "Estresa denboran",    "short",   "stress_score",   "Estresa",     GOR),
]
# ── Info técnica (panel 13, visible en Grafana directo pero no en el web app) ──
op.append({
    "id": 13, "type": "table", "title": "Ezaugarri teknikoak",
    "gridPos": {"x": 0, "y": 28, "w": 24, "h": 4}, "datasource": ds(),
    "targets": [tgt(
        "SELECT nombre, procesador, ram, almacenamiento, ultima_vez "
        "FROM ordenadores WHERE empresa_id = $empresa AND nombre = '$nombre'"
    )],
    "transformations": [{"id": "organize", "options": {
        "renameByName": {
            "nombre": "Ordenagailua", "procesador": "Prozesadorea",
            "ram": "RAM", "almacenamiento": "Diskoa", "ultima_vez": "Azken konexioa"
        }
    }}],
    "fieldConfig": {"defaults": {"unit": "string", "custom": {"align": "auto", "cellOptions": {"type": "auto"}}}, "overrides": [
        {"matcher": {"id": "byName", "options": "Azken konexioa"},
         "properties": [{"id": "unit", "value": "dateTimeAsLocal"}]}
    ]},
    "options": {"showHeader": True, "cellHeight": "sm", "footer": {"show": False}},
})

for pid, x, y, title, unit, col, lbl, steps in TS_PANELS:
    cql = (f"SELECT nombre, {col}, ts FROM mediciones "
           f"WHERE empresa_id = $empresa AND nombre = '$nombre' "
           f"AND ts > $__timeFrom AND ts < $__timeTo ALLOW FILTERING")
    op.append({
        "id": pid, "type": "timeseries", "title": title,
        "gridPos": {"x": x, "y": y, "w": 12, "h": 8}, "datasource": ds(),
        "targets": [tgt(cql, fmt="time_series")],
        "transformations": [{"id": "organize", "options": {
            "excludeByName": {"nombre": True},
            "renameByName": {"ts": "Ordua", col: lbl}
        }}],
        "fieldConfig": {"defaults": {"unit": unit, "displayName": lbl, "thresholds": thresh(steps)}, "overrides": []},
        "options": {
            "legend": {"displayMode": "table", "placement": "bottom",
                       "calcs": ["lastNotNull", "mean", "max", "min"]},
            "tooltip": {"mode": "single", "sort": "none"}
        },
    })

# ─────────────────────────────────────────────────────────────────────────────
# Escribir ficheros JSON
# ─────────────────────────────────────────────────────────────────────────────
BASE = "c:/Users/User/Downloads/WebHardMon_cassandra_mysql/grafana-local/provisioning/dashboards/json/"
for fname, dash in [
    ("webhardmon-empresa.json",    empresa),
    ("webhardmon-ordenadores.json", ordenador),
]:
    with open(BASE + fname, "w", encoding="utf-8") as f:
        json.dump(dash, f, indent=2, ensure_ascii=False)
    print(f"OK: {fname}")
