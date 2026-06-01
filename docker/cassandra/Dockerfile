FROM cassandra:5.0

ENV CASSANDRA_CLUSTER_NAME=webhardmon \
    CASSANDRA_DC=datacenter1 \
    CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch

# Schema y seed copiados desde la raíz del proyecto
COPY schema-cassandra.cql /init/schema.cql
COPY seed-cassandra.cql   /init/seed.cql

# Script que arranca Cassandra, espera a que esté lista y aplica los CQL
COPY docker/cassandra/entrypoint.sh /entrypoint.sh
RUN sed -i 's/\r//' /entrypoint.sh && chmod +x /entrypoint.sh

EXPOSE 9042

ENTRYPOINT ["/entrypoint.sh"]
