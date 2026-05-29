# Migracion a MySQL + Cassandra

## Cambios aplicados

- PostgreSQL se ha sustituido por MySQL en `pom.xml` y `src/main/resources/application.yml`.
- `VictoriaMetricsService` se ha sustituido por `CassandraTelemetryService`.
- Grafana ahora usa el plugin `hadesarchitect-cassandra-datasource`.
- Se han cambiado los dashboards provisionados para apuntar al datasource `cassandra`.
- Se anade `schema-cassandra.cql` con las tablas necesarias para telemetria.

## Arranque local

```bash
cd grafana-local
docker compose up -d
```

Cuando Cassandra este lista:

```bash
docker exec -i webhardmon-cassandra cqlsh < ../schema-cassandra.cql
docker exec -i webhardmon-cassandra cqlsh < ../seed-cassandra.cql
```

## Variables principales

La app usa MySQL para usuarios/licencias/empresas y Cassandra para detectar portatiles con metricas. Revisa `.env.example`.

## Importante

Los dashboards convertidos usan CQL de base. Es posible que haya que ajustar visualizaciones concretas en Grafana porque PromQL (`avg`, `topk`, `last_over_time`) no existe en Cassandra; esas agregaciones deben hacerse con consultas CQL, vistas/materialized tables o transformaciones de Grafana.
