FROM mysql:8.4

# Defaults — override at runtime with -e or --env-file
ENV MYSQL_DATABASE=telemetriadb \
    MYSQL_USER=admin \
    MYSQL_PASSWORD=secret \
    MYSQL_ROOT_PASSWORD=rootsecret

# MySQL ejecuta automáticamente todo lo que haya en /docker-entrypoint-initdb.d/
# al crear el volumen por primera vez (en orden alfabético).
COPY schema-mysql.sql          /docker-entrypoint-initdb.d/01-schema.sql
COPY docker/mysql/matomo-db.sql /docker-entrypoint-initdb.d/02-matomo-db.sql

EXPOSE 3306
