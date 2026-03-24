FROM docker-dbc.artifacts.dbccloud.dk/dbc-java${java.rt.version}:latest
LABEL org.opencontainers.image.authors="${team}"

ENV TZ=Europe/Copenhagen

COPY target/dataio-cli-datafile-exporter.jar /work/
COPY target/dataio-cli-job-replicator.jar /work/