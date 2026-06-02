FROM matomo:5

# Conexión a MySQL — sobreescribir en runtime si es necesario
ENV MATOMO_DATABASE_HOST=webhardmon-mysql \
    MATOMO_DATABASE_ADAPTER=mysqli \
    MATOMO_DATABASE_TABLES_PREFIX=matomo_ \
    MATOMO_DATABASE_USERNAME=admin \
    MATOMO_DATABASE_PASSWORD=secret \
    MATOMO_DATABASE_DBNAME=matomodb

EXPOSE 80
